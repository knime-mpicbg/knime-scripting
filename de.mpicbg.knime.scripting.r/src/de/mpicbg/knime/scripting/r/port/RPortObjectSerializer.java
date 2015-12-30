package de.mpicbg.knime.scripting.r.port;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;

public final class RPortObjectSerializer extends PortObjectSerializer<RPortObject> {
	
	private static final String ZIP_ENTRY_WS = "Rworkspace.RData";

	@Override
	public void savePortObject(RPortObject portObject, PortObjectZipOutputStream out, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		out.putNextEntry(new ZipEntry(ZIP_ENTRY_WS));
		Files.copy(portObject.getFile().toPath(), out);
		out.flush();
		out.closeEntry();
		out.close();
	}

	@Override
	public RPortObject loadPortObject(PortObjectZipInputStream in, PortObjectSpec spec, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		ZipEntry nextEntry = in.getNextEntry();
		if ((nextEntry == null) || !nextEntry.getName().equals(ZIP_ENTRY_WS)) {
			throw new IOException("Expected zip entry '" + ZIP_ENTRY_WS + "' not present");
		}
		
		File tempFile = File.createTempFile("genericR", ".RData");
		Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		in.close();
		return new RPortObject(tempFile);
	}

}
