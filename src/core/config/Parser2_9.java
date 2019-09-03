package core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import argo.jdom.JsonNode;
import argo.jdom.JsonNodeFactories;
import argo.jdom.JsonRootNode;
import core.cli.server.CliServer;
import core.controller.CoreConfig;
import core.ipc.IPCServiceManager;
import core.ipc.IPCServiceName;
import core.ipc.repeatClient.repeatPeerClient.RepeatsPeerServiceClientManager;
import core.keyChain.KeyChain;
import core.userDefinedTask.TaskGroup;
import core.userDefinedTask.internals.ToolsConfig;
import utilities.json.JSONUtility;

public class Parser2_9 extends ConfigParser {

	private static final Logger LOGGER = Logger.getLogger(Parser2_9.class.getName());

	@Override
	protected String getVersion() {
		return "2.9";
	}

	@Override
	protected String getPreviousVersion() {
		return "2.8";
	}

	@Override
	protected JsonRootNode internalConvertFromPreviousVersion(JsonRootNode previousVersion) {
		JsonNode globalConfig = previousVersion.getNode("global_settings");
		globalConfig = JSONUtility.addChild(globalConfig, "tools_config", JsonNodeFactories.array(JsonNodeFactories.string("local"))).getRootNode();
		globalConfig = JSONUtility.addChild(globalConfig, "core_config", JsonNodeFactories.array(JsonNodeFactories.string("local"))).getRootNode();
		previousVersion = JSONUtility.replaceChild(previousVersion, "global_settings", globalConfig).getRootNode();
		previousVersion = JSONUtility.addChild(previousVersion, "remote_repeats_clients",
				JsonNodeFactories.object(
						JsonNodeFactories.field(JsonNodeFactories.string("clients"), JsonNodeFactories.array()))).getRootNode();

		JsonNode compilers = previousVersion.getNode("compilers");
		compilers = JsonNodeFactories.object(
				JsonNodeFactories.field("local_compilers", compilers),
				JsonNodeFactories.field("remote_repeats_compilers", JsonNodeFactories.array(JsonNodeFactories.string("local")))
				);
		previousVersion = JSONUtility.replaceChild(previousVersion, "compilers", compilers).getRootNode();

		List<JsonNode> groups = previousVersion.getArrayNode("task_groups");
		List<JsonNode> newGroups = new ArrayList<>();
		for (JsonNode group : groups) {
			List<JsonNode> tasks = group.getArrayNode("tasks");
			List<JsonNode> newTasks = new ArrayList<>();
			for (JsonNode task : tasks) {
				JsonNode taskWithId = JSONUtility.addChild(task, "action_id", JsonNodeFactories.string(UUID.randomUUID().toString()));
				newTasks.add(taskWithId);
			}

			JsonNode newGroup = JSONUtility.replaceChild(group, "tasks", JsonNodeFactories.array(newTasks));
			newGroup = JSONUtility.addChild(newGroup, "group_id", JsonNodeFactories.string(UUID.randomUUID().toString()));
			newGroups.add(newGroup);
		}

		return JSONUtility.replaceChild(previousVersion, "task_groups", JsonNodeFactories.array(newGroups)).getRootNode();
	}

	@Override
	protected boolean internalExtractData(Config config, JsonRootNode root) {
		try {
			JsonNode globalSettings = root.getNode("global_settings");
			config.setUseTrayIcon(globalSettings.getBooleanValue("tray_icon_enabled"));
			config.setEnabledHaltingKeyPressed(globalSettings.getBooleanValue("enabled_halt_by_key"));
			config.setExecuteOnKeyReleased(globalSettings.getBooleanValue("execute_on_key_released"));
			config.setUseClipboardToTypeString(globalSettings.getBooleanValue("use_clipboard_to_type_string"));
			config.setNativeHookDebugLevel(Level.parse(globalSettings.getNode("debug").getStringValue("level")));


			JsonNode globalHotkey = globalSettings.getNode("global_hotkey");

			String mouseGestureActivation = globalHotkey.getNumberValue("mouse_gesture_activation");
			config.setMouseGestureActivationKey(Integer.parseInt(mouseGestureActivation));
			config.setRECORD(KeyChain.parseJSON(globalHotkey.getArrayNode("record")));
			config.setREPLAY(KeyChain.parseJSON(globalHotkey.getArrayNode("replay")));
			config.setCOMPILED_REPLAY(KeyChain.parseJSON(globalHotkey.getArrayNode("replay_compiled")));

			JsonNode toolsConfigNode = globalSettings.getNode("tools_config");
			ToolsConfig toolsConfig = ToolsConfig.parseJSON(toolsConfigNode);
			config.setToolsConfig(toolsConfig);

			JsonNode coreConfigNode = globalSettings.getNode("core_config");
			CoreConfig coreConfig = CoreConfig.parseJSON(coreConfigNode);
			config.setCoreConfig(coreConfig);

			JsonNode peerClients = root.getNode("remote_repeats_clients");
			RepeatsPeerServiceClientManager repeatsPeerServiceClientManager = RepeatsPeerServiceClientManager.parseJSON(peerClients);
			config.getBackEnd().getPeerServiceClientManager().updateClients(repeatsPeerServiceClientManager.getClients());

			List<JsonNode> ipcSettings = root.getArrayNode("ipc_settings");
			if (!IPCServiceManager.parseJSON(ipcSettings)) {
				LOGGER.log(Level.WARNING, "IPC Service Manager failed to parse JSON metadata");
			}

			if (!config.getCompilerFactory().parseJSON(root.getNode("compilers"))) {
				LOGGER.log(Level.WARNING, "Dynamic Compiler Manager failed to parse JSON metadata");
			}

			config.getBackEnd().clearTaskGroup();
			for (JsonNode taskGroupNode : root.getArrayNode("task_groups")) {
				TaskGroup taskGroup = TaskGroup.parseJSON(config.getCompilerFactory(), taskGroupNode);
				if (taskGroup != null) {
					config.getBackEnd().addTaskGroup(taskGroup);
				}
			}

			if (config.getBackEnd().getTaskGroups().isEmpty()) {
				config.getBackEnd().addTaskGroup(new TaskGroup("default"));
			}
			config.getBackEnd().setCurrentTaskGroup(config.getBackEnd().getTaskGroups().get(0));
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to parse json", e);
			return false;
		}
	}

	@Override
	protected boolean internalImportData(Config config, JsonRootNode root) {
		boolean result = true;

		for (JsonNode taskGroupNode : root.getArrayNode("task_groups")) {
			TaskGroup taskGroup = TaskGroup.parseJSON(config.getCompilerFactory(), taskGroupNode);
			result &= taskGroup != null;
			if (taskGroup != null) {
				result &= config.getBackEnd().addPopulatedTaskGroup(taskGroup);
			}
		}
		return result;
	}

	@Override
	protected boolean internalExtractData(CliConfig config, JsonRootNode root) {
		try {
			List<JsonNode> ipcSettings = root.getArrayNode("ipc_settings");
			if (!IPCServiceManager.parseJSON(ipcSettings)) {
				LOGGER.log(Level.WARNING, "IPC Service Manager failed to parse JSON metadata");
			}

			CliServer cliServer = (CliServer) IPCServiceManager.getIPCService(IPCServiceName.CLI_SERVER);
			config.setServerPort(cliServer.getPort());
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to parse json", e);
			return false;
		}
	}
}