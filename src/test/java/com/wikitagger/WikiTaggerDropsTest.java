package com.wikitagger;

import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

public class WikiTaggerDropsTest {

    private WikiTaggerPlugin plugin;

    @Before
    public void initializeTestEnvironment() throws ReflectiveOperationException {
        this.plugin = new WikiTaggerPlugin();
        Field httpClient = WikiTaggerPlugin.class.getDeclaredField("httpClient");
        httpClient.setAccessible(true);
        httpClient.set(this.plugin, new OkHttpClient());
    }

    @Test
    public void testDropsByMonster() {
        Assert.assertTrue(
                "Failed to query by monster",
                plugin.getDropIDs("imp").length > 0);
        Assert.assertTrue(
                "Failed to query by monster",
                plugin.getDropIDs("rune dragon").length > 0);
        Assert.assertTrue(
                "Failed to query by monster",
                plugin.getDropIDs("rune_dragon").length > 0);
    }

}
