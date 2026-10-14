package com.csci318.recommendationservice;

import com.csci318.recommendationservice.domain.Specification;
import com.csci318.recommendationservice.service.CatalogProduct;

import java.util.Arrays;
import java.util.List;

/**
 * Catalog products used by the tests, with the canonical category and specification names.
 * The cheapest possible build costs 960.00; {@link #VALID_BUILD} costs 990.00.
 */
public final class TestCatalog {

    public static final CatalogProduct CPU_AM5 = product("cpu-am5", "Ryzen 5 7600", "AMD", "CPU", 200f,
            spec("Socket", "AM5"));
    public static final CatalogProduct CPU_LGA1700 = product("cpu-lga1700", "Core i5-13400", "Intel", "CPU", 190f,
            spec("Socket", "LGA1700"));
    public static final CatalogProduct MOTHERBOARD = product("mb-b650", "B650 Board", "ASUS", "Motherboard", 150f,
            spec("Socket", "AM5"), spec("Memory Type", "DDR5"), spec("Form Factor", "ATX"));
    public static final CatalogProduct RAM = product("ram-ddr5", "32GB DDR5", "Corsair", "RAM", 100f,
            spec("Memory Type", "DDR5"));
    public static final CatalogProduct GPU = product("gpu-4060", "RTX 4060", "NVIDIA", "GPU", 300f,
            spec("Memory", "8GB"));
    public static final CatalogProduct GPU_CHEAP = product("gpu-7600", "RX 7600", "AMD", "GPU", 280f,
            spec("Memory", "8GB"));
    public static final CatalogProduct STORAGE = product("ssd-1tb", "1TB NVMe SSD", "Samsung", "Storage", 80f,
            spec("Capacity", "1TB"));
    public static final CatalogProduct PSU = product("psu-750", "750W PSU", "Seasonic", "PSU", 90f,
            spec("Wattage", "750W"));
    public static final CatalogProduct CASE = product("case-atx", "ATX Tower", "Fractal", "Case", 70f,
            spec("Form Factor", "ATX"));
    public static final CatalogProduct COOLER = product("cooler-1", "Tower Cooler", "Noctua", "Cooler", 30f,
            spec("Height", "150mm"));

    public static final List<String> VALID_BUILD = List.of(CPU_AM5.id(), MOTHERBOARD.id(), RAM.id(), GPU.id(),
            STORAGE.id(), PSU.id(), CASE.id());

    private TestCatalog() {
    }

    public static List<CatalogProduct> all() {
        return List.of(CPU_AM5, CPU_LGA1700, MOTHERBOARD, RAM, GPU, GPU_CHEAP, STORAGE, PSU, CASE, COOLER);
    }

    public static CatalogProduct product(String id, String name, String brand, String category, float price,
                                         Specification... specifications) {
        return new CatalogProduct(id, name, brand, price, category, Arrays.asList(specifications));
    }

    public static Specification spec(String name, String value) {
        return new Specification(name, value);
    }
}
