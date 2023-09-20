package com.wikitagger;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.runelite.client.plugins.banktags.BankTagsPlugin.*;

@Slf4j
@PluginDescriptor(
	name = "Bank Tag WikiTagger"
)
@PluginDependency(
		value = BankTagsPlugin.class
)

public class WikiTaggerPlugin extends Plugin
{

	private static final String WIKI_API = "https://oldschool.runescape.wiki/api.php?action=ask&query=%s|+limit=2000&format=json";

	@Inject
	private Client client;

	@Inject
	private WikiTaggerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private TagManager tagManager;

	@Inject
	private OkHttpClient httpClient;

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted) {
		String[] args = commandExecuted.getArguments();
		if (commandExecuted.getCommand().equals(config.dropsChatCommand()) && args.length > 0) {
			addTagsFromDrops(String.join(" ", args));
		}
	}

	@Provides
	WikiTaggerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(WikiTaggerConfig.class);
	}

	/**
	 * Created a tag based off specified monster drops
	 *
	 * @param monster The name of the osrs wiki category to generate a list of items to tag.
	 */
	private void addTagsFromDrops(String monster) {
		log.info("Attempting to add tags to items dropped by {}", monster);
		int[] items = getDropIDs(monster);
		tagItems(items, monster + " drops");
		if (items.length == 0) {
			String message = String.format("No drops found for %s", monster);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
		} else {
			String message = String.format("Added %s drops tag to %s items.", monster, items.length);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			createTab(monster + " drops", items[0]);
		}
	}

	/**
	 * Bank Tag created for items.
	 *
	 * @param items Item ID's to be tagged
	 * @param tag   Tag to be applied to the items
	 */
	private void tagItems(int[] items, String tag) {
		for (int itemID : items) {
			tagManager.addTag(itemID, tag, false);
		}
	}

	/**
	 * Applies a BankTag tag to the provided items
	 *
	 * @return A list of bank tabs in string format.
	 */
	private List<String> getAllTabs() {
		return Text.fromCSV(MoreObjects.firstNonNull(configManager.getConfiguration(CONFIG_GROUP, TAG_TABS_CONFIG), ""));
	}

	/**
	 * Creates a new Bank Tab
	 *
	 * @param tag        Name of the bank tag
	 * @param iconItemId Item ID of the item to be the tab icon
	 */
	private void createTab(String tag, int iconItemId) {
		// Bank tags config must be change directly as TagManager is not public
		//String currentConfig = configManager.getConfiguration(CONFIG_GROUP, TAG_TABS_CONFIG);

		List<String> tabs = new ArrayList<>(getAllTabs());
		tabs.add(Text.standardize(tag));
		String tags = Text.toCSV(tabs);

		configManager.setConfiguration(CONFIG_GROUP, TAG_TABS_CONFIG, tags);
		configManager.setConfiguration(CONFIG_GROUP, ICON_SEARCH + Text.standardize(tag), iconItemId);

	}

	/**
	 * Gather item IDs of all drops by the specified monster
	 *
	 * @param monster Name of the OSRS monster that will be Item Ids will be generated from
	 * @return List of Item IDs found for the provided category.
	 */
	int[] getDropIDs(String monster) {
		try {
			String safe_query = URLEncoder.encode(monster, "UTF-8");
			String query = String.format("[[Dropped from::%s]]|?Dropped item.All+Item+ID", safe_query);
			String wikiResponse = Objects.requireNonNull(getWikiResponse(query).body()).string();
			return getIDsFromJSON(wikiResponse);
		} catch (IOException e) {
			if (client != null)
				client.addChatMessage(ChatMessageType.GAMEMESSAGE,
						"",
						"There was an error retrieving data",
						"");
			log.error(e.getMessage());
			return new int[0];
		}
	}

	/**
	 * Query the Old School RuneScape Wiki and returns the response
	 *
	 * @param category Category query string
	 * @return Results of the query
	 */
	private Response getWikiResponse(String category) throws IOException {
		Request request = new Request.Builder()
				.url(createQueryURL(category))
				.build();
		return httpClient.newCall(request).execute();
	}

	/**
	 * Constructs the URL of the specified query string
	 *
	 * @param query Query to be used
	 * @return Full query URL
	 */
	private String createQueryURL(String query) {
		return String.format(WIKI_API, query);
	}

	/**
	 * Extracts ItemIDs from a JSON HTTP response.
	 *
	 * @param jsonIn JSON as a string. It must be in the correct format.
	 * @return List of the item IDs pulled from the JSON results.
	 * @see AskQuery.Response
	 */
	private int[] getIDsFromJSON(String jsonIn) {
		Gson gson = new Gson();
		AskQuery.Response askResponse = gson.fromJson(jsonIn, AskQuery.Response.class);
		return askResponse.getQuery().getResults().values()
				.stream()
				.flatMap(v -> v.getPrintouts().getAllItemID().stream())
				.mapToInt(x -> x)
				.distinct()
				.toArray();
	}
}