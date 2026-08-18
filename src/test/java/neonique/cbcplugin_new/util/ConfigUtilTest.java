package neonique.cbcplugin_new.util;

import neonique.cbcplugin_new.testutil.TestUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigUtilTest {

    @Test
    public void testIntegerRequired () {
        String configString = "test_int: 20";
        Configuration config = TestUtil.configFromString(configString);

        int result = ConfigUtil.requireInt(config, "test_int");

        assertEquals(20, result);
    }

    @Test
    public void testIntegerRequiredWithFloat () {
        String configString = "test_int: 20.5";
        Configuration config = TestUtil.configFromString(configString);

        assertThrows(ConfigUtil.InvalidConfigValueException.class, () -> ConfigUtil.requireInt(config, "test_int"));
    }

    @Test
    public void testStringRequired () {

        String configString = "test_str: hello";
        Configuration config = TestUtil.configFromString(configString);

        String result = ConfigUtil.requireString(config, "test_str");

        assertEquals("hello", result);

    }

    @Test
    public void testVectorRequired () {

        String configString = "test_vec: [20.0, 0.5, 20.0]";
        Configuration config = TestUtil.configFromString(configString);

        Vector result = ConfigUtil.requireVector(config, "test_vec");

        assertEquals(new Vector(20.0, 0.5, 20.0), result);

    }

    @Test
    public void testVectorListRequired () {

        String configString = """
                test_vec_list:
                - [1.0, 2.0, 3.0]
                - [4.0, 5.0, 6.0]
                - [7.0, 8.0, 9.0]
                """;
        Configuration config = TestUtil.configFromString(configString);

        List<Vector> result = ConfigUtil.requireVectorList(config, "test_vec_list");

        assertAll(
                () -> assertEquals(3, result.size()),
                () -> assertEquals(new Vector(1.0, 2.0, 3.0), result.get(0)),
                () -> assertEquals(new Vector(4.0, 5.0, 6.0), result.get(1)),
                () -> assertEquals(new Vector(7.0, 8.0, 9.0), result.get(2))
        );

    }

}
