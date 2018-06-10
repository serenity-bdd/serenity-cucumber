package net.serenitybdd.cucumber.integration;

import cucumber.api.TypeRegistry;
import cucumber.api.TypeRegistryConfigurer;
import io.cucumber.datatable.DataTableType;
import net.serenitybdd.cucumber.integration.steps.RpnCalculatorStepdefs;

import java.util.Locale;
import java.util.Map;

public class TypeRegistryConfiguration implements TypeRegistryConfigurer {

    @Override
    public Locale locale() {
        return Locale.ENGLISH;
    }

    @Override
    public void configureTypeRegistry(TypeRegistry typeRegistry) {
        typeRegistry.defineDataTableType(new DataTableType(
                RpnCalculatorStepdefs.Entry.class,
                (Map<String, String> row) -> new RpnCalculatorStepdefs.Entry(
                        Integer.parseInt(row.get("first")),
                        Integer.parseInt(row.get("second")),
                        row.get("operation"))
                )
        );
    }
}
