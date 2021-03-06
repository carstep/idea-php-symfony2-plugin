package fr.adrienbrault.idea.symfony2plugin.tests.util.yaml;


import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.junit.Assert;
import org.junit.Test;

public class YamlHelperTest extends Assert {

    @Test
    public void testisValidParameterName() {
        assertTrue(YamlHelper.isValidParameterName("%a%"));
        assertTrue(YamlHelper.isValidParameterName("%a.a%"));
        assertTrue(YamlHelper.isValidParameterName("%a.-_a%"));

        assertFalse(YamlHelper.isValidParameterName("%%"));
        assertFalse(YamlHelper.isValidParameterName(""));
        assertFalse(YamlHelper.isValidParameterName("%"));
        assertFalse(YamlHelper.isValidParameterName("%kernel.root_dir%/../web/"));
        assertFalse(YamlHelper.isValidParameterName("%kernel.root_dir%/../web/%"));
        assertFalse(YamlHelper.isValidParameterName("%kernel.root_dir%/../web/%webpath_modelmasks%"));
    }

}
