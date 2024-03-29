/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.python2.port;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * {@link FileStore}-based port object containing a {@link PickledObject}.
 * <P>
 * Note that this class implements two kinds of serialization for pickled objects.
 * <ul>
 * <li><b>old-style</b>: Bytes from Python + additional info written to a output stream. This introduces unnecessary
 * overhead and is therefore deprecated. The implementation is still present to support previously saved port
 * objects.</li>
 * <li><b>new-style</b>: File is written directly from Python.</li>
 * </ul>
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 3.6.1
 */
public final class PickledObjectFileStorePortObject extends FileStorePortObject {

    /**
     * The type of this port.
     */
    @SuppressWarnings("hiding")
    public static final PortType TYPE =
        PortTypeRegistry.getInstance().getPortType(PickledObjectFileStorePortObject.class);

    /**
     * Global cache for all pickled objects.
     */
    private static final MemoryAlertAwareGuavaCache CACHE = MemoryAlertAwareGuavaCache.getInstance();

    private final PickledObjectPortObjectSpec m_spec;

    private final UUID m_key;

    // effectively final
    private PickledObjectFile m_pickledObjectFile;

    /**
     * @param pickledObject the pickled object to save
     * @param fileStore the file store at which to save the pickled object
     * @throws IOException if failed to write the pickled object to file store
     * @deprecated use {@link #create(FileStoreFactory, PickledObjectFileProducer)} instead
     */
    @Deprecated
    public PickledObjectFileStorePortObject(final PickledObject pickledObject, final FileStore fileStore)
        throws IOException {
        super(Arrays.asList(fileStore));
        m_spec = new PickledObjectPortObjectSpec(pickledObject.getType(), pickledObject.getStringRepresentation());
        m_key = UUID.randomUUID();
        CACHE.put(m_key, pickledObject);
        flushToFileStore();
        m_pickledObjectFile = null;
    }

    private PickledObjectFileStorePortObject(final PickledObjectFile pickledObjectFile, final FileStore fileStore) {
        super(List.of(fileStore));
        m_spec = new PickledObjectPortObjectSpec(pickledObjectFile.getType(),
            pickledObjectFile.getRepresentation());
        m_pickledObjectFile = pickledObjectFile;
        // PickledObjectFileStores are light weight and therefore not cached, hence we need no key for retrieval
        m_key = null;
    }

    /**
     * Creates an instance of this PortObject.
     *
     * @param fileStoreFactory for creating file stores (see {@link FileStoreFactory#createFileStoreFactory(ExecutionContext)})
     * @param pickledObjectFileProducer creates the {@link PickledObjectFile} given a {@link File}
     * @return a new instance
     * @throws IOException
     * @throws CanceledExecutionException
     */
    public static PickledObjectFileStorePortObject create(final FileStoreFactory fileStoreFactory,
        final PickledObjectFileProducer pickledObjectFileProducer) throws IOException, CanceledExecutionException {
        final var fileStore = fileStoreFactory.createFileStore(UUID.randomUUID().toString());
        var pickledObjectFile = pickledObjectFileProducer.produce(fileStore.getFile());
        return new PickledObjectFileStorePortObject(pickledObjectFile, fileStore);
    }

    /**
     * Essentially a function that given a {@link File} produces a {@link PickledObjectFile} and may throw certain exceptions.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public interface PickledObjectFileProducer {
        /**
         * Produces a {@link PickledObjectFile} given a {@link File} to pickle to.
         *
         * @param file to pickle the object to
         * @return the PickledObjectFile
         * @throws CanceledExecutionException if the user cancels the node execution
         * @throws IOException if there are I/O issues on Python side
         */
        PickledObjectFile produce(final File file) throws CanceledExecutionException, IOException;
    }

    /**
     * Deserialization constructor for version 2 (the pickled objects are held in the filestore and never deserialized
     * in Java)
     */
    private PickledObjectFileStorePortObject(final PickledObjectPortObjectSpec spec) {
        m_spec = spec;
        m_key = null;
    }

    @Override
    protected void postConstruct() throws IOException {
        super.postConstruct();
        m_pickledObjectFile =
            new PickledObjectFile(getFileStore(0).getFile(), m_spec.getType(), m_spec.getRepresentation());
    }

    /**
     * Deserialization constructor.
     */
    private PickledObjectFileStorePortObject(final PickledObjectPortObjectSpec spec, final UUID key) {
        m_spec = spec;
        m_key = key;
        m_pickledObjectFile = null;
    }

    /**
     * @return the contained {@link PickledObject}
     * @throws IOException if the pickled object needed to be loaded from file store, which failed
     */
    @Deprecated
    public synchronized PickledObject getPickledObject() throws IOException {
        if (m_key != null) {
            try {
                return CACHE.get(m_key, this::getPickledObjectFromFileStore);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException)e.getCause();
                } else {
                    throw new IllegalStateException("Failed to load pickled object.", e);
                }
            }
        } else {
            return PickledObject.fromPickledObjectFile(m_pickledObjectFile);
        }
    }

    @SuppressWarnings("deprecation")
    private PickledObject getPickledObjectFromFileStore() throws IOException {
        final var file = getFileStore(0).getFile();
        try (var in = new FileInputStream(file)) {
            return new PickledObject(in);
        }
    }

    /**
     * Retrieves the {@link PickledObjectFile} stored in the PortObject.
     *
     * @return the {@link PickledObjectFile}
     * @throws IOException if creating the {@link PickledObjectFile} from an old {@link PickledObject} fails
     */
    @SuppressWarnings("deprecation")
    public PickledObjectFile getPickledObjectFile() throws IOException {
        if (m_key != null) {
            // means that we have an old PickledObject on our hands
            return getPickledObject().toPickledObjectFile();
        } else {
            return m_pickledObjectFile;
        }
    }

    @Override
    public String getSummary() {
        return shortenString(m_spec.getType() + "\n" + m_spec.getRepresentation(), 60, "...");
    }

    @Override
    public PortObjectSpec getSpec() {
        return m_spec;
    }

    @Override
    public JComponent[] getViews() {
        var pickledObjectString = m_spec.getRepresentation();
        pickledObjectString = shortenString(pickledObjectString, 1000, "\n...");
        String text = "<html><b>" + m_spec.getType() + "</b><br><br><code>"
                + pickledObjectString.replace("\n", "<br>") + "</code></html>";
        final var label = new JLabel(text);
        final var font = label.getFont();
        final var plainFont = new Font(font.getFontName(), Font.PLAIN, font.getSize());
        label.setFont(plainFont);
        final var panel = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(label, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy++;
        gbc.weighty = Double.MIN_VALUE;
        gbc.weightx = Double.MIN_VALUE;
        panel.add(new JLabel(), gbc);
        final JComponent f = new JScrollPane(panel);
        f.setName("Pickled object");
        return new JComponent[]{f};
    }

    private static String shortenString(final String string, final int maxLength, final String suffix) {
        return string.length() > maxLength ? (string.substring(0, maxLength - suffix.length()) + suffix) : string;
    }

    @Override
    public int hashCode() {
        if (m_pickledObjectFile != null) {
            // hashing on the type and representation ensures that objects that are considered to be equal have
            // the same hashcode while still being relatively fast at the cost of a higher risk of hash collisions
            // Those then need to be resolved using equals which will load the byte[] representing the
            // pickled object into memory.
            return Objects.hash(m_pickledObjectFile.getType(), m_pickledObjectFile.getRepresentation());
        } else {
            try {
                return Objects.hashCode(getPickledObject());
            } catch (final IOException ex) {
                throw new IllegalStateException("Failed to load pickled object.", ex);
            }
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PickledObjectFileStorePortObject)) {
            return false;
        }
        final PickledObjectFileStorePortObject other = (PickledObjectFileStorePortObject)obj;
        try {
            return Objects.equals(getPickledObject(), other.getPickledObject());
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load pickled object.", ex);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void flushToFileStore() throws IOException {
        final File file = getFileStore(0).getFile();
        if (m_pickledObjectFile != null) {
            // If we have new-style port objects m_pickledObjectFile is never null.
            // In this case we have to do nothing because the file was already written by Python directly.
            // To be robust - we copy it if we did not write to the same file but this should not happen.
            if (!m_pickledObjectFile.getFile().equals(file)) {
                Files.copy(m_pickledObjectFile.getFile().toPath(), file.toPath());
            }
        } else {
            // If we have old-style port objects we use the deprecated saving mechanism
            // based on getPickledObject
            try (FileOutputStream out = new FileOutputStream(file)) {
                getPickledObject().save(out);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (m_key != null) {
            CACHE.remove(m_key);
        }
    }

    /**
     * Serializer of {@link PickledObjectFileStorePortObject}.
     */
    public static final class Serializer extends PortObjectSerializer<PickledObjectFileStorePortObject> {

        private static final String ZIP_ENTRY_NAME = "PickledObjectFileStorePortObject";

        @Override
        public void savePortObject(final PickledObjectFileStorePortObject portObject,
            final PortObjectZipOutputStream out, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            out.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
            var dataOut = new DataOutputStream(out);
            // Save "version" for forward compatibility reasons.
            if (portObject.m_key != null) {
                dataOut.writeInt(1);
                dataOut.writeUTF(portObject.m_key.toString());
            } else {
                dataOut.writeInt(2);
                // key is no longer used
            }
            dataOut.flush();
        }

        @Override
        public PickledObjectFileStorePortObject loadPortObject(final PortObjectZipInputStream in,
            final PortObjectSpec spec, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            final ZipEntry entry = in.getNextEntry();
            if (!ZIP_ENTRY_NAME.equals(entry.getName())) {
                throw new IOException("Failed to load pickled object port object. Invalid zip entry name '"
                    + entry.getName() + "', expected '" + ZIP_ENTRY_NAME + "'.");
            }
            var dataIn = new DataInputStream(in);
            int version = dataIn.readInt(); // NOSONAR
            if (version == 1) {
                var key = UUID.fromString(dataIn.readUTF());
                return new PickledObjectFileStorePortObject((PickledObjectPortObjectSpec)spec, key);
            } else if (version == 2) {
                return new PickledObjectFileStorePortObject((PickledObjectPortObjectSpec)spec);
            } else {
                throw new IllegalStateException("Unsupported version: " + version);
            }
        }
    }
}
