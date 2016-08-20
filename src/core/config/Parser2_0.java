package core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import utilities.JSONUtility;
import argo.jdom.JsonNode;
import argo.jdom.JsonNodeFactories;
import argo.jdom.JsonRootNode;

import com.sun.glass.events.KeyEvent;

import core.ipc.IPCServiceManager;
import core.keyChain.KeyChain;
import core.userDefinedTask.TaskGroup;

public class Parser2_0 extends ConfigParser {

	private static final Logger LOGGER = Logger.getLogger(Parser2_0.class.getName());

	@Override
	protected String getVersion() {
		return "2.0";
	}

	@Override
	protected String getPreviousVersion() {
		return "1.9";
	}

	@Override
	protected JsonRootNode internalConvertFromPreviousVersion(JsonRootNode previousVersion) {
		try {
			// Add mouse gesture activation to global hotkey
			JsonNode globalSettings = previousVersion.getNode("global_settings");
			JsonNode globalHotkey = globalSettings.getNode("global_hotkey");

			globalHotkey = JSONUtility.addChild(
								globalHotkey,
								"mouse_gesture_activation",
								JsonNodeFactories.number(KeyEvent.VK_SHIFT));

			globalSettings = JSONUtility.replaceChild(globalSettings, "global_hotkey", globalHotkey);

			JsonNode output = JSONUtility.replaceChild(previousVersion, "global_settings", globalSettings).getRootNode();

			// Modify hotkey into activation
			List<JsonNode> taskGroups = output.getArrayNode("task_groups");
			List<JsonNode> convertedTaskGroup = new ArrayList<>();
			for (JsonNode group : taskGroups) {
				List<JsonNode> tasks = group.getArrayNode("tasks");
				List<JsonNode> convertedTasks = new ArrayList<>();
				for (JsonNode task : tasks) {
					JsonNode hotkey = task.getNode("hotkey");
					JsonNode activation = JsonNodeFactories.object(
							JsonNodeFactories.field("hotkey", hotkey),
							JsonNodeFactories.field("mouse_gesture", JsonNodeFactories.array()));

					task = JSONUtility.addChild(task, "activation", activation);
					convertedTasks.add(task);
				}


				group = JSONUtility.replaceChild(group, "tasks", JsonNodeFactories.array(convertedTasks));
				convertedTaskGroup.add(group);
			}
			output = JSONUtility.replaceChild(output, "task_groups", JsonNodeFactories.array(convertedTaskGroup));

			return output.getRootNode();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to convert json from previous version " + getPreviousVersion(), e);
			return null;
		}
	}

	@Override
	protected boolean internalExtractData(Config config, JsonRootNode root) {
		try {
			JsonNode globalSettings = root.getNode("global_settings");
			config.setUseTrayIcon(globalSettings.getBooleanValue("tray_icon_enabled"));
			config.setEnabledHaltingKeyPressed(globalSettings.getBooleanValue("enabled_halt_by_key"));
			config.setExecuteOnKeyReleased(globalSettings.getBooleanValue("execute_on_key_released"));
			config.setNativeHookDebugLevel(Level.parse(globalSettings.getNode("debug").getStringValue("level")));

			JsonNode globalHotkey = globalSettings.getNode("global_hotkey");

			String mouseGestureActivation = globalHotkey.getNumberValue("mouse_gesture_activation");
			config.setMouseGestureActivationKey(Integer.parseInt(mouseGestureActivation));
			config.setRECORD(KeyChain.parseJSON(globalHotkey.getArrayNode("record")));
			config.setREPLAY(KeyChain.parseJSON(globalHotkey.getArrayNode("replay")));
			config.setCOMPILED_REPLAY(KeyChain.parseJSON(globalHotkey.getArrayNode("replay_compiled")));

			List<JsonNode> ipcSettings = root.getArrayNode("ipc_settings");
			if (!IPCServiceManager.parseJSON(ipcSettings)) {
				LOGGER.log(Level.WARNING, "IPC Service Manager failed to parse JSON metadata");
			}

			if (!config.getCompilerFactory().parseJSON(root.getArrayNode("compilers"))) {
				LOGGER.log(Level.WARNING, "Dynamic Compiler Manager failed to parse JSON metadata");
			}

			List<TaskGroup> taskGroups = config.getBackEnd().getTaskGroups();
			taskGroups.clear();
			for (JsonNode taskGroupNode : root.getArrayNode("task_groups")) {
				TaskGroup taskGroup = TaskGroup.parseJSON(config.getCompilerFactory(), taskGroupNode);
				if (taskGroup != null) {
					taskGroups.add(taskGroup);
				}
			}

			if (taskGroups.isEmpty()) {
				taskGroups.add(new TaskGroup("default"));
			}
			config.getBackEnd().setCurrentTaskGroup(taskGroups.get(0));
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to parse json", e);
			return false;
		}
	}

	@Override
	protected boolean internalImportData(Config config, JsonRootNode root) {
		boolean result = true;

		List<TaskGroup> taskGroups = config.getBackEnd().getTaskGroups();
		for (JsonNode taskGroupNode : root.getArrayNode("task_groups")) {
			TaskGroup taskGroup = TaskGroup.parseJSON(config.getCompilerFactory(), taskGroupNode);
			result &= taskGroup != null;
			if (taskGroup != null) {
				taskGroups.add(taskGroup);
			}
		}
		return result;
	}
}