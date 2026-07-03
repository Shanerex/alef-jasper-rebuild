package com.alef.api.office.controller;

import com.alef.api.office.dto.OfficeDto;
import com.alef.api.office.dto.OfficesDto;
import com.alef.api.office.service.OfficeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for GET /api/offices (architecture §3.2, feature 011).
 *
 * Verifies status code, JSON envelope shape, and array fields (phones,
 * addressLines) with a mocked service layer.
 */
@WebMvcTest(OfficeController.class)
class OfficeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OfficeService service;

    @Test
    void getOffices_returns_offices_envelope_with_arrays() throws Exception {
        var dubai = new OfficeDto(
                "dubai",
                "ALEF — Dubai (Head Office)",
                List.of("404 Sheikh Maktoum Building", "Damascus Street, Al Qusais", "PO Box 65825, Dubai, UAE"),
                List.of("+971 4 2513840", "+971 4 3434440"),
                "alefllc@eim.ae",
                "Sheikh Maktoum Building, Damascus Street, Al Qusais, Dubai");
        var india = new OfficeDto(
                "india",
                "Jasper — India (Delivery Centre)",
                List.of("Plot No. 2/286, Near Dharani Sugars", "Vasudevanallur, Tirunelveli Dist.", "Tamil Nadu 627 758, India"),
                List.of("0091 4636 293166"),
                "jasperiec@gmail.com",
                "Vasudevanallur, Tirunelveli District, Tamil Nadu 627758");
        when(service.getOffices()).thenReturn(new OfficesDto(List.of(dubai, india)));

        mvc.perform(get("/api/offices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offices", hasSize(2)))
                .andExpect(jsonPath("$.offices[0].key").value("dubai"))
                .andExpect(jsonPath("$.offices[0].name").value("ALEF — Dubai (Head Office)"))
                .andExpect(jsonPath("$.offices[0].addressLines", hasSize(3)))
                .andExpect(jsonPath("$.offices[0].addressLines[0]").value("404 Sheikh Maktoum Building"))
                .andExpect(jsonPath("$.offices[0].phones", hasSize(2)))
                .andExpect(jsonPath("$.offices[0].phones[0]").value("+971 4 2513840"))
                .andExpect(jsonPath("$.offices[0].phones[1]").value("+971 4 3434440"))
                .andExpect(jsonPath("$.offices[0].email").value("alefllc@eim.ae"))
                .andExpect(jsonPath("$.offices[0].mapQuery").value("Sheikh Maktoum Building, Damascus Street, Al Qusais, Dubai"))
                .andExpect(jsonPath("$.offices[1].key").value("india"))
                .andExpect(jsonPath("$.offices[1].phones", hasSize(1)));
    }

    @Test
    void getOffices_returns_empty_offices_array_when_no_rows() throws Exception {
        when(service.getOffices()).thenReturn(new OfficesDto(List.of()));

        mvc.perform(get("/api/offices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offices", hasSize(0)));
    }
}
