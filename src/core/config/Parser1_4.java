package core.config;

import java.util.logging.Level;
import java.util.logging.Logger;

import utilities.Function;
import utilities.JSONUtility;
import argo.jdom.JsonNode;
import argo.jdom.JsonNodeFactories;
import argo.jdom.JsonRootNode;

public class Parser1_4 extends ConfigParser {

	private static final Logger LOGGER = Logger.getLogger(Parser1_4.class.getName());

	@Override
	protected String getVersion() {
		return "1.4";
	}

	@Override
	protected String getPreviousVersion() {
		return "1.3";
	}

	@Override
	protected JsonRootNode internalConvertFromPreviousVersion(JsonRootNode previousVersion) {
		try {
			JsonNode taskGroup = JsonNodeFactories.array(new Function<JsonNode, JsonNode>(){
				@Override
				public JsonNode apply(JsonNode taskGroup) {
					return JSONUtility.replaceChild(taskGroup, "tasks", JsonNodeFactories.array(
							new Function<JsonNode, JsonNode>() {
								@Override
								public JsonNode apply(JsonNode task) {
									return JSONUtility.replaceChild(task, "hotkey",
											JsonNodeFactories.array(task.getNode("hotkey")));
								}
							}.map(taskGroup.getArrayNode("tasks"))));
				}
			}.map(previousVersion.getArrayNode("task_groups")));

			JsonRootNode newRoot = JsonNodeFactories.object(
					JsonNodeFactories.field("version", JsonNodeFactories.string(getVersion())),
					JsonNodeFactories.field("global_hotkey", previousVersion.getNode("global_hotkey")),
					JsonNodeFactories.field("compilers", previousVersion.getNode("compilers")),
					JsonNodeFactories.field("task_groups", taskGroup)
					);
			return newRoot;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to convert json from previous version " + getPreviousVersion(), e);
			return null;
		}
	}

	@Override
	protected boolean internalExtractData(Config config, JsonRootNode root) {
		try {
			//Convert to 1_5
			Parser1_5 parser = new Parser1_5();
			JsonRootNode newRoot = parser.convertFromPreviousVersion(root);

			if (newRoot != null) {
				return parser.extractData(config, newRoot);
			} else {
				LOGGER.log(Level.WARNING, "Unable to convert to later version " + parser.getVersion());
				return false;
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to parse json", e);
			return false;
		}
	}

	@Override
	protected boolean importData(Config config, JsonRootNode data) {
		LOGGER.warning("Unsupported import data at version " + getVersion());
		return false;
	}
}