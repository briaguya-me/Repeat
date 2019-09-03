package core.userDefinedTask;

import java.util.logging.Level;

import core.controller.Core;
import core.languageHandler.Language;
import core.languageHandler.compiler.AbstractNativeCompiler;
import core.languageHandler.compiler.DynamicCompilerOutput;
import utilities.ILoggable;
import utilities.Pair;

public final class DormantUserDefinedTask extends UserDefinedAction implements ILoggable {

	private final String source;

	public DormantUserDefinedTask(String source, Language compiler) {
		this.source = source;
		this.compiler = compiler;
	}

	@Override
	public final void action(Core controller) throws InterruptedException {
		getLogger().log(Level.WARNING, "Task " + name + " is dormant. Recompile to use it.");
	}

	@Override
	public String getSource() {
		if (source != null) {
			return source;
		} else {
			return super.getSource();
		}
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public void setEnabled(boolean enabled) {
		getLogger().log(Level.WARNING, "Task " + name + "is dormant. Recompile to enable it.");
	}

	@Override
	public UserDefinedAction recompileNative(AbstractNativeCompiler compiler) {
		Pair<DynamicCompilerOutput, UserDefinedAction> result = compiler.compile(source, getCompiler());
		DynamicCompilerOutput compilerStatus = result.getA();
		UserDefinedAction output = result.getB();
		output.actionId = getActionId();

		if (compilerStatus != DynamicCompilerOutput.COMPILATION_SUCCESS) {
			getLogger().warning("Unable to recompile dormant task " + getName() + ". Error " + compilerStatus);
			return this;
		}
		getLogger().info("Successfully recompiled dormant task " + getName() + ".");
		output.syncContent(this);
		output.compiler = getCompiler();
		return output;
	}
}
