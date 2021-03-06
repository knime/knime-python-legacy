/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 17, 2020 (marcel): created
 */
package org.knime.python2.kernel;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.knime.conda.Conda;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.kernel.PythonKernelBackendRegistry.PythonKernelBackendType;
import org.knime.python2.prefs.advanced.PythonAdvancedPreferences;
import org.knime.python2.util.PythonUtils;

import com.google.common.collect.Iterables;

/**
 * Creates, holds, and provides {@link PythonKernel} instances for specific combinations of {@link PythonCommand Python
 * commands} and (preloaded) additional Python modules. Clients can retrieve these instances, one at a time, via
 * {@link #getNextKernel(PythonCommand, Set, Set, PythonKernelOptions, PythonCancelable) getNextKernel}. Upon retrieval
 * of an instance from the queue, a new kernel is automatically and asynchronously created and enqueued to keep the
 * queue evenly populated.
 * <P>
 * The queue only holds a limited number of kernels. It evicts and {@link PythonKernelQueue#close() closes} inactive
 * kernels (i.e., kernels that have been idling for a specific time) in case the number of entries reaches this limit.
 * It also regularly evicts and closes inactive kernel instances independent of the current number of entries.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class PythonKernelQueue {

    static {
        // If a new environment has been created (either overwriting an existing environment or
        // "overwriting" a previously non-existent environment), the entries in the kernel queue that
        // reference the old environment are rendered obsolete and therefore need to be invalidated. The
        // same is true in case the environment creation failed, which likely leaves the environment in a
        // corrupt state and also needs to be reflected by the queue.
        // Unfortunately, clearing only the entries of the queue that reference the old environment is not
        // straightforwardly done in the queue's current implementation, therefore we need to clear the
        // entire queue for now.
        Conda.registerEnvironmentChangeListener(event -> clear());
    }

    // Class:

    /**
     * The default maximum number of idling kernels that are held by the queue at any time, that is, the default
     * capacity of the queue.
     */
    public static final int DEFAULT_MAX_NUMBER_OF_IDLING_KERNELS = 3;

    /**
     * The default duration after which unused idling kernels are marked as expired. The default duration until expired
     * entries are actually evicted is generally longer than this value because the underlying pool performs clean-ups
     * in a timer-based manner. The clean-up interval of the timer is governed by
     * {@code EVICTION_CHECK_INTERVAL_IN_MILLISECONDS}.
     */
    public static final int DEFAULT_EXPIRATION_DURATION_IN_MINUTES = 5;

    private static final int EVICTION_CHECK_INTERVAL_IN_MILLISECONDS = 60 * 1000;

    private static final int CANCELLATION_CHECK_INTERVAL_IN_MILLISECONDS = 1000;

    /**
     * The singleton instance.
     */
    private static PythonKernelQueue instance;

    /**
     * Takes the next {@link PythonKernel} from the queue that was launched using the given {@link PythonCommand}, uses
     * the old kernel back end, and has the given modules preloaded. Configures it according to the given
     * {@link PythonKernelOptions} and returns it. The caller is responsible for {@link PythonKernel#close() closing}
     * the kernel.<br>
     * This method blocks until a kernel is present in the queue.
     * <P>
     * Note that specifying additional modules should only be done if loading these modules is time-consuming since,
     * internally, an own queue will be registered for each combination of Python command and modules. Having too many
     * of these combinations could defeat the purpose of queuing since all these queues will compete for the limited
     * number of available slots in the overall queue.
     *
     * @param command The {@link PythonCommand}.
     * @param requiredAdditionalModules The modules that must already be loaded in the returned kernel. May not be
     *            {@code null}, but empty.
     * @param optionalAdditionalModules The modules that should already -- but do not need to -- be loaded in the
     *            returned kernel. May not be {@code null}, but empty.
     * @param options The {@link PythonKernelOptions} according to which the returned {@link PythonKernel} is
     *            configured.
     * @param cancelable The {@link PythonCancelable} that is used to check whether retrieving the kernel from the queue
     *            (i.e., waiting until a kernel is present in the queue) should be canceled.
     * @return The next appropriate {@link PythonKernel} in the queue, configured according to the given
     *         {@link PythonKernelOptions}.
     * @throws PythonCanceledExecutionException If retrieving the kernel has been canceled or
     *             {@link InterruptedException interrupted}.
     * @throws PythonIOException The exception that was thrown while originally constructing the kernel that is next in
     *             the queue, if any. Such exceptions are preserved and rethrown by this method in order to make calling
     *             this method equivalent to constructing the kernel directly, from an exception-delivery point of view.
     */
    public static synchronized PythonKernel getNextKernel(final PythonCommand command,
        final Set<PythonModuleSpec> requiredAdditionalModules, final Set<PythonModuleSpec> optionalAdditionalModules,
        final PythonKernelOptions options, final PythonCancelable cancelable)
        throws PythonCanceledExecutionException, PythonIOException {
        return getNextKernel(command, PythonKernelBackendType.PYTHON2, requiredAdditionalModules,
            optionalAdditionalModules, options, cancelable);
    }

    /**
     * Takes the next {@link PythonKernel} from the queue that was launched using the given {@link PythonCommand}, uses
     * the given kernel back end, and has the given modules preloaded. Configures it according to the given
     * {@link PythonKernelOptions} and returns it. The caller is responsible for {@link PythonKernel#close() closing}
     * the kernel.<br>
     * This method blocks until a kernel is present in the queue.
     * <P>
     * Note that specifying additional modules should only be done if loading these modules is time-consuming since,
     * internally, an own queue will be registered for each combination of Python command and modules. Having too many
     * of these combinations could defeat the purpose of queuing since all these queues will compete for the limited
     * number of available slots in the overall queue.
     *
     * @param command The {@link PythonCommand}.
     * @param kernelBackendType The type of the {@link PythonKernelBackend kernel back end} to use.
     * @param requiredAdditionalModules The modules that must already be loaded in the returned kernel. May not be
     *            {@code null}, but empty.
     * @param optionalAdditionalModules The modules that should already -- but do not need to -- be loaded in the
     *            returned kernel. May not be {@code null}, but empty.
     * @param options The {@link PythonKernelOptions} according to which the returned {@link PythonKernel} is
     *            configured.
     * @param cancelable The {@link PythonCancelable} that is used to check whether retrieving the kernel from the queue
     *            (i.e., waiting until a kernel is present in the queue) should be canceled.
     * @return The next appropriate {@link PythonKernel} in the queue, configured according to the given
     *         {@link PythonKernelOptions}.
     * @throws PythonCanceledExecutionException If retrieving the kernel has been canceled or
     *             {@link InterruptedException interrupted}.
     * @throws PythonIOException The exception that was thrown while originally constructing the kernel that is next in
     *             the queue, if any. Such exceptions are preserved and rethrown by this method in order to make calling
     *             this method equivalent to constructing the kernel directly, from an exception-delivery point of view.
     */
    public static synchronized PythonKernel getNextKernel(final PythonCommand command,
        final PythonKernelBackendType kernelBackendType, final Set<PythonModuleSpec> requiredAdditionalModules,
        final Set<PythonModuleSpec> optionalAdditionalModules, final PythonKernelOptions options,
        final PythonCancelable cancelable) throws PythonCanceledExecutionException, PythonIOException {
        if (instance == null) {
            reconfigureKernelQueue(PythonAdvancedPreferences.getMaximumNumberOfIdlingProcesses(),
                PythonAdvancedPreferences.getExpirationDurationInMinutes());
        }
        return instance.getNextKernelInternal(command, kernelBackendType, requiredAdditionalModules,
            optionalAdditionalModules, options, cancelable);
    }

    /**
     * Reconfigures the queue according to the given arguments.
     * <P>
     * Implementation note: we do not expect the queue to be reconfigured regularly. Therefore we do not reconfigure it
     * while it is being in use but simply {@link #close() close} it and reinstantiate it using the provided arguments.
     *
     * @param maxNumberOfIdlingKernels The maximum number of idling kernels that are held by the queue at any time, that
     *            is, the capacity of the queue.
     * @param expirationDurationInMinutes The duration in minutes after which unused idling kernels are marked as
     *            expired. The duration until expired entries are actually evicted is generally longer than this value
     *            because the underlying pool performs clean-ups in a timer-based manner. The clean-up interval of the
     *            timer is governed by {@code EVICTION_CHECK_INTERVAL_IN_MILLISECONDS}.
     */
    public static synchronized void reconfigureKernelQueue(final int maxNumberOfIdlingKernels,
        final int expirationDurationInMinutes) {
        final boolean sameConfiguration = instance != null //
            && instance.m_pool.getMaxTotal() == maxNumberOfIdlingKernels
            && instance.m_pool.getMinEvictableIdleTimeMillis() == expirationDurationInMinutes * 60l * 1000l;
        if (!sameConfiguration) {
            close();
            instance = new PythonKernelQueue(maxNumberOfIdlingKernels, expirationDurationInMinutes);
        }
    }

    /**
     * Removes all {@link PythonKernel Python kernels} from the queue and closes them.
     */
    public static synchronized void clear() {
        if (instance != null) {
            instance.m_pool.clear();
        }
    }

    /**
     * Closes all contained {@link PythonKernel Python kernels} and clears the queue. Calling
     * {@link #getNextKernel(PythonCommand, Set, Set, PythonKernelOptions, PythonCancelable) getNextKernel} without
     * calling {@link #reconfigureKernelQueue(int, int) reconfigureKernelQueue} first is not allowed.
     */
    public static synchronized void close() {
        if (instance != null) {
            instance.m_pool.close();
        }
    }

    // Instance:

    /**
     * An object pool that is used as a collection of queues of {@link PythonKernel Python kernels}, each queue indexed
     * by the command and sets of preloaded Python modules for which it holds kernel instances.
     * <P>
     * The semantic differences between queues (where clients take items without returning them) and pools (borrowing
     * and returning items) is bridged by not pooling kernels directly but rather wrapping them in reusable containers,
     * extracting them upon borrowing, and returning and repopulating the containers.
     */
    private final GenericKeyedObjectPool<PythonKernelSpec, PythonKernelOrExceptionHolder> m_pool;

    private PythonKernelQueue(final int maxNumberOfIdlingKernels, final int expirationDurationInMinutes) {
        final GenericKeyedObjectPoolConfig<PythonKernelOrExceptionHolder> config = new GenericKeyedObjectPoolConfig<>();
        config.setEvictorShutdownTimeoutMillis(0);
        config.setFairness(true);
        config.setJmxEnabled(false);
        config.setLifo(false);
        config.setMaxIdlePerKey(-1);
        config.setMaxTotal(maxNumberOfIdlingKernels);
        config.setMaxTotalPerKey(-1);
        config.setMaxWaitMillis(CANCELLATION_CHECK_INTERVAL_IN_MILLISECONDS);
        config.setMinEvictableIdleTimeMillis(expirationDurationInMinutes * 60l * 1000l);
        config.setNumTestsPerEvictionRun(-1);
        config.setTimeBetweenEvictionRunsMillis(EVICTION_CHECK_INTERVAL_IN_MILLISECONDS);
        m_pool = new GenericKeyedObjectPool<>(new KeyedPooledPythonKernelFactory(), config);
    }

    @SuppressWarnings("resource") // Kernel is closed by the client.
    private PythonKernel getNextKernelInternal(final PythonCommand command,
        final PythonKernelBackendType kernelBackendType, final Set<PythonModuleSpec> requiredAdditionalModules,
        final Set<PythonModuleSpec> optionalAdditionalModules, final PythonKernelOptions options,
        final PythonCancelable cancelable) throws PythonCanceledExecutionException, PythonIOException {
        final PythonKernelSpec key =
            new PythonKernelSpec(command, kernelBackendType, requiredAdditionalModules, optionalAdditionalModules);
        if (m_pool.getMaxTotal() != 0) {
            final PythonKernelOrExceptionHolder holder = dequeueHolder(key, cancelable);
            PythonKernel kernel = extractKernelAndEnqueueNewOne(key, holder);
            kernel = configureOrRecreateKernel(key, kernel, options);
            return kernel;
        } else {
            // Otherwise we need to bypass the queue since there are no slots that we could use.
            return createKernelAndConfigure(key, options);
        }
    }

    @SuppressWarnings("resource") // Holder was not taken from pool when this method throws.
    private PythonKernelOrExceptionHolder dequeueHolder(final PythonKernelSpec key, final PythonCancelable cancelable)
        throws PythonCanceledExecutionException {
        PythonKernelOrExceptionHolder holder = null;
        do {
            try {
                holder = m_pool.borrowObject(key);
            } catch (final NoSuchElementException ex) { // NOSONAR Timeout is expected and part of control flow.
                cancelable.checkCanceled();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new PythonCanceledExecutionException(ex);
            } catch (final Exception ex) {
                // Should not happen. The only significant source of further checked exceptions is the object-pool
                // factory. And our implementation of the factory (see below) does not throw any checked exceptions.
                throw new IllegalStateException(ex);
            }
        } while (holder == null);
        return holder;
    }

    private PythonKernel extractKernelAndEnqueueNewOne(final PythonKernelSpec key,
        final PythonKernelOrExceptionHolder holder) throws PythonIOException {
        try {
            return holder.clearFieldsAndReturnKernelOrThrow();
        } finally {
            new Thread(() -> m_pool.returnObject(key, holder), "python-kernel-creator").start();
        }
    }

    /**
     * Setting options may fail if the Python process crashed between adding it to the queue and now taking it from the
     * queue. We try to recover from such a situation by opening and configuring a new kernel (once). Note that we
     * naturally do not try to recover from a failed installation test since this requires user action. However, it is
     * our responsibility to close the kernel in any exceptional situation here since the client will not have a handle
     * to the kernel.
     */
    private static PythonKernel configureOrRecreateKernel(final PythonKernelSpec key, PythonKernel kernel,
        final PythonKernelOptions options) throws PythonIOException {
        try {
            kernel.setOptions(options);
        } catch (final PythonInstallationTestException ex) {
            PythonUtils.Misc.closeSafelyThrowErrors(null, kernel);
            throw ex;
        } catch (final PythonIOException ex) {
            PythonUtils.Misc.closeSafelyThrowErrors(null, kernel);
            try {
                kernel = createKernelAndConfigure(key, options);
            } catch (final Throwable t) { // NOSONAR
                t.addSuppressed(ex);
                throw t;
            }
        } catch (final Throwable t) { // NOSONAR
            PythonUtils.Misc.closeSafelyThrowErrors(null, kernel);
            throw t;
        }
        return kernel;
    }

    private static PythonKernel createKernelAndConfigure(final PythonKernelSpec key, final PythonKernelOptions options)
        throws PythonIOException {
        final PythonKernel kernel = KeyedPooledPythonKernelFactory.createKernel(key);
        try {
            kernel.setOptions(options);
        } catch (final Throwable t) { // NOSONAR
            PythonUtils.Misc.closeSafelyThrowErrors(null, kernel);
            throw t;
        }
        return kernel;
    }

    /**
     * Manages the life-cycle of pooled kernel holders. In particular:
     * <ul>
     * <li>creates and populates new holders when the pool contains no idling instances and
     * {@link PythonKernelQueue#getNextKernelInternal(PythonCommand, Set, Set, PythonKernelOptions, PythonCancelable)}
     * is called</li>
     * <li>repopulates holders when they are returned to the pool after extracting their kernel in
     * {@link PythonKernelQueue#extractKernelAndEnqueueNewOne(PythonKernelSpec, PythonKernelOrExceptionHolder)}</li>
     * <li>closes kernels if their holders are evicted</li
     * </ul>
     */
    private static final class KeyedPooledPythonKernelFactory
        implements KeyedPooledObjectFactory<PythonKernelSpec, PythonKernelOrExceptionHolder> {

        @Override
        @SuppressWarnings("resource") // No kernel is held yet.
        public PooledObject<PythonKernelOrExceptionHolder> makeObject(final PythonKernelSpec key) {
            final PythonKernelOrExceptionHolder holder = new PythonKernelOrExceptionHolder();
            populateHolder(key, holder);
            return new DefaultPooledObject<>(holder);
        }

        @Override
        public void passivateObject(final PythonKernelSpec key, final PooledObject<PythonKernelOrExceptionHolder> p) {
            @SuppressWarnings("resource") // No kernel is held yet or any more.
            final PythonKernelOrExceptionHolder holder = p.getObject();
            if (holder.m_kernel == null && holder.m_exception == null) {
                populateHolder(key, holder);
            }
        }

        private static void populateHolder(final PythonKernelSpec key, final PythonKernelOrExceptionHolder holder) {
            try {
                holder.m_kernel = createKernel(key);
            } catch (final PythonIOException ex) {
                holder.m_exception = ex;
            }
        }

        @SuppressWarnings("resource") // Back end will be closed along with kernel.
        private static PythonKernel createKernel(final PythonKernelSpec key) throws PythonIOException {
            PythonKernelBackend kernelBackend;
            try {
                kernelBackend =
                    PythonKernelBackendRegistry.getBackend(key.m_kernelBackendType).createBackend(key.m_command);
            } catch (final PythonIOException ex) {
                throw ex;
            } catch (final IOException ex) {
                throw new PythonIOException(ex);
            }
            final PythonKernel kernel = new PythonKernel(kernelBackend);
            try {
                loadAdditionalModules(key, kernel);
            } catch (final PythonIOException ex) {
                PythonUtils.Misc.closeSafelyThrowErrors(null, kernel);
                throw ex;
            } catch (final Exception ex) { // NOSONAR
                PythonUtils.Misc.closeSafelyThrowErrors(null, kernel);
                throw new PythonIOException(ex);
            } catch (final Throwable t) { // NOSONAR
                PythonUtils.Misc.closeSafelyThrowErrors(null, kernel);
                throw t;
            }
            return kernel;
        }

        private static void loadAdditionalModules(final PythonKernelSpec key, final PythonKernel kernel)
            throws PythonIOException {
            final Set<PythonModuleSpec> requiredModules = key.m_requiredAdditionalModules;
            if (!requiredModules.isEmpty()) {
                PythonKernel.testInstallation(key.m_command, requiredModules);
                final String requiredModulesImportCode =
                    String.join("\n", Iterables.transform(requiredModules, m -> "import " + m.getName()));
                kernel.execute(requiredModulesImportCode);
            }
            final Set<PythonModuleSpec> optionalModules = key.m_optionalAdditionalModules;
            if (!optionalModules.isEmpty()) {
                final String optionalModulesImportCode = String.join("\n", Iterables.transform(optionalModules, m -> //
                /* */ "try:\n" //
                    + "\timport " + m.getName() + "\n" //
                    + "except Exception:\n" //
                    + "\tpass" //
                ));
                kernel.execute(optionalModulesImportCode);
            }
        }

        @SuppressWarnings("resource") // We are literally closing the object here.
        @Override
        public void destroyObject(final PythonKernelSpec key, final PooledObject<PythonKernelOrExceptionHolder> p) {
            PythonUtils.Misc.closeSafelyThrowErrors(null, p.getObject());
        }

        @Override
        public boolean validateObject(final PythonKernelSpec key, final PooledObject<PythonKernelOrExceptionHolder> p) {
            // Nothing to do.
            return true;
        }

        @Override
        public void activateObject(final PythonKernelSpec key, final PooledObject<PythonKernelOrExceptionHolder> p) {
            // Nothing to do.
        }
    }

    private static final class PythonKernelSpec {

        private final PythonCommand m_command;

        private final PythonKernelBackendType m_kernelBackendType;

        private final Set<PythonModuleSpec> m_requiredAdditionalModules;

        private final Set<PythonModuleSpec> m_optionalAdditionalModules;

        private final int m_hashCode;

        public PythonKernelSpec(final PythonCommand command, final PythonKernelBackendType kernelBackendType,
            final Set<PythonModuleSpec> requiredAdditionalModules,
            final Set<PythonModuleSpec> optionalAdditionalModules) {
            m_command = command;
            m_kernelBackendType = kernelBackendType;
            // Make defensive copies since instances of this class are used as keys in the underlying pool. Preserve
            // order of modules if this matters when loading them.
            m_requiredAdditionalModules = new LinkedHashSet<>(requiredAdditionalModules);
            m_optionalAdditionalModules = new LinkedHashSet<>(optionalAdditionalModules);
            m_hashCode = Objects.hash(command, kernelBackendType, requiredAdditionalModules, optionalAdditionalModules);
        }

        @Override
        public int hashCode() {
            return m_hashCode;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof PythonKernelSpec)) {
                return false;
            }
            final PythonKernelSpec other = (PythonKernelSpec)obj;
            return other.m_command.equals(m_command) //
                && other.m_kernelBackendType == m_kernelBackendType //
                && other.m_requiredAdditionalModules.equals(m_requiredAdditionalModules) //
                && other.m_optionalAdditionalModules.equals(m_optionalAdditionalModules);
        }
    }

    private static final class PythonKernelOrExceptionHolder implements AutoCloseable {

        private PythonKernel m_kernel;

        private PythonIOException m_exception;

        public PythonKernel clearFieldsAndReturnKernelOrThrow() throws PythonIOException {
            final PythonKernel kernel = m_kernel;
            m_kernel = null;
            final PythonIOException exception = m_exception;
            m_exception = null;
            if (exception != null) {
                throw exception;
            } else {
                return kernel;
            }
        }

        @Override
        public void close() throws PythonKernelCleanupException {
            if (m_kernel != null) {
                m_kernel.close();
            }
        }
    }
}
