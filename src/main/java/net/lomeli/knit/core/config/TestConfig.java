package net.lomeli.knit.core.config;

import net.lomeli.knit.core.config.annotations.Config;

public class TestConfig {
    @Config(comment = "This is a comment")
    public static String test_string = "this is a test";

    @Config(comment = "This is a comment", minValue = 0, maxValue = 10)
    public static int test_int = 5;

    @Config(category = "TestCategory", categoryComment = "This is a category comment")
    public static boolean test_boolean = true;
}
