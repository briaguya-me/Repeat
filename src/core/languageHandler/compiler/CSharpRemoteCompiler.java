package core.languageHandler.compiler;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import argo.jdom.JsonNode;
import argo.jdom.JsonNodeFactories;
import core.languageHandler.Language;

public class CSharpRemoteCompiler extends AbstractRemoteNativeCompiler {

	private File path;

	{
		getLogger().setLevel(Level.ALL);
	}

	public CSharpRemoteCompiler(File objectFileDirectory) {
		super(objectFileDirectory);
		path = new File(".");
	}

	@Override
	public boolean canSetPath() {
		return false;
	}

	@Override
	public boolean setPath(File file) {
		this.path = file;
		return true;
	}

	@Override
	public File getPath() {
		return path;
	}

	@Override
	public Language getName() {
		return Language.CSHARP;
	}

	@Override
	public String getExtension() {
		return ".cs";
	}

	@Override
	public String getObjectExtension() {
		return ".dll";
	}

	@Override
	public boolean parseCompilerSpecificArgs(JsonNode node) {
		return true;
	}

	@Override
	public JsonNode getCompilerSpecificArgs() {
		return JsonNodeFactories.object();
	}

	@Override
	protected String getDummyPrefix() {
		return "CS_";
	}

	@Override
	public Logger getLogger() {
		return Logger.getLogger(CSharpRemoteCompiler.class.getName());
	}

	@Override
	protected boolean checkRemoteCompilerSettings() {
		return true;
	}
}
