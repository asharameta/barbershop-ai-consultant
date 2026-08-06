package com.asharameta.barbershop.knowledgebase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BarbershopFileParserTest {
    static Stream<Arguments> validCases(){
        return Stream.of(
                Arguments.of("shop-name__city-name__category.txt","shop-name", "city-name", "category"),
                Arguments.of("shop__city__category.txt","shop", "city", "category"),
                Arguments.of("shop__city__category.TXT","shop", "city", "category"),
                Arguments.of("Shop__City__Category.txt","Shop", "City", "Category"),
                Arguments.of("SHOP__CITY__CATEGORY.txt","SHOP", "CITY", "CATEGORY")
        );
    }

    static Stream<Arguments> invalidCases(){
        return Stream.of(
                Arguments.of("shop__city__category.csv"),
                Arguments.of("shop__city__category.md"),
                Arguments.of("shop__city__category"),
                Arguments.of("shop_city_category.txt"),
                Arguments.of("shop____category.txt"),
                Arguments.of("shop__city_category"),
                Arguments.of(""),
                Arguments.of((Object) null),
                Arguments.of("null"),
                Arguments.of("shop___city___category.txt"),
                Arguments.of("shop--city--category"),
                Arguments.of("shop__category.txt"),
                Arguments.of("shop__city__street__category.txt"),
                Arguments.of("shop__city__category.txt.txt"),
                Arguments.of("shop_name__city_name__category.txt")

        );
    }

    @DisplayName("Valid filenames parse into correct BarbershopMetadata")
    @ParameterizedTest
    @MethodSource("validCases")
    void parseFileName_validInput_returnsMetadata(String filename, String name, String city, String category){
        BarbershopFileParser.BarbershopMetadata barbershopMetadata = BarbershopFileParser.parseFileName(filename);

        assertNotNull(barbershopMetadata);
        assertEquals(name, barbershopMetadata.name());
        assertEquals(city, barbershopMetadata.city());
        assertEquals(category, barbershopMetadata.category());
    }

    @DisplayName("Invalid filenames return null")
    @ParameterizedTest
    @MethodSource("invalidCases")
    void parseFileName_invalidInput_returnsNull(String filename){
        assertNull(BarbershopFileParser.parseFileName(filename));
    }

}