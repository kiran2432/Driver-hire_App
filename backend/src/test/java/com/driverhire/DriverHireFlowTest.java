package com.driverhire;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end API tests against the real Spring context, security filter chain, Flyway schema and an
 * in-memory H2 database. The database is shared by all tests in this class, so every test uses its own
 * unique emails and city name.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriverHireFlowTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    MockMvc mvc;

    // ---- auth ------------------------------------------------------------------------------------

    @Test
    void customerCanRegisterLoginAndReadOwnProfile() throws Exception {
        String email = uniqueEmail("cust");
        mvc.perform(postJson("/api/auth/register/customer", customerJson(email, "9876543210", "Pune")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(header().string("Set-Cookie", containsString("dh_refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")));

        String body = mvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.customer.city").value("Pune"))
                .andExpect(jsonPath("$.driver").value(nullValue()));
    }

    @Test
    void duplicateEmailAndWeakPasswordAreRejected() throws Exception {
        String email = uniqueEmail("dup");
        mvc.perform(postJson("/api/auth/register/customer", customerJson(email, "9876543210", "Pune")))
                .andExpect(status().isCreated());
        mvc.perform(postJson("/api/auth/register/customer", customerJson(email, "9876543210", "Pune")))
                .andExpect(status().isConflict());

        String weak = customerJson(uniqueEmail("weak"), "9876543210", "Pune").replace("Password123", "short");
        mvc.perform(postJson("/api/auth/register/customer", weak))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameGenericError() throws Exception {
        String email = uniqueEmail("login");
        mvc.perform(postJson("/api/auth/register/customer", customerJson(email, "9876543210", "Pune")))
                .andExpect(status().isCreated());

        mvc.perform(postJson("/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"WrongPass999\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        mvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"nobody-" + SEQ.incrementAndGet() + "@example.com\",\"password\":\"WrongPass999\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void refreshRotatesTokensAndReuseRevokesTheSession() throws Exception {
        MvcResult reg = mvc.perform(postJson("/api/auth/register/customer",
                        customerJson(uniqueEmail("refresh"), "9876543210", "Pune")))
                .andExpect(status().isCreated()).andReturn();
        String first = reg.getResponse().getCookie("dh_refresh").getValue();

        MvcResult rotated = mvc.perform(post("/api/auth/refresh").cookie(new Cookie("dh_refresh", first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String second = rotated.getResponse().getCookie("dh_refresh").getValue();

        // Replaying the old token is treated as theft...
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("dh_refresh", first)))
                .andExpect(status().isUnauthorized());
        // ...and kills the newer token too.
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("dh_refresh", second)))
                .andExpect(status().isUnauthorized());
        // No cookie at all.
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        MvcResult reg = mvc.perform(postJson("/api/auth/register/customer",
                        customerJson(uniqueEmail("logout"), "9876543210", "Pune")))
                .andExpect(status().isCreated()).andReturn();
        String refresh = reg.getResponse().getCookie("dh_refresh").getValue();

        mvc.perform(post("/api/auth/logout").cookie(new Cookie("dh_refresh", refresh)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("dh_refresh", refresh)))
                .andExpect(status().isUnauthorized());
    }

    // ---- authorisation -----------------------------------------------------------------------------

    @Test
    void endpointsRequireAuthenticationAndTheRightRole() throws Exception {
        String city = uniqueCity();
        String customer = registerCustomer(uniqueEmail("c"), "9876543210", city);
        String driver = registerDriver(uniqueEmail("d"), "9123456789", city, true, true);

        mvc.perform(get("/api/drivers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trips/mine")).andExpect(status().isUnauthorized());

        mvc.perform(auth(get("/api/drivers"), driver)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/trips/open"), customer)).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/trips/1/accept"), customer)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/customers/me"), driver)).andExpect(status().isForbidden());
    }

    // ---- hiring flow -------------------------------------------------------------------------------

    @Test
    void customerHiresDriverAndContactDetailsAreSharedOnlyAfterConfirmation() throws Exception {
        String city = uniqueCity();
        String customer = registerCustomer(uniqueEmail("c"), "9876543210", city);
        String driver = registerDriver(uniqueEmail("d"), "9123456789", city, true, true);

        long tripId = createTrip(customer, city, "ROUND_TRIP", "AUTOMATIC");

        // Search finds the driver and hides private details.
        String search = mvc.perform(auth(get("/api/drivers").param("city", city.toUpperCase())
                        .param("transmission", "AUTOMATIC"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].licenseNumber").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long driverId = ((Number) JsonPath.read(search, "$.content[0].id")).longValue();

        // Hire -> REQUESTED, still no phone numbers.
        mvc.perform(auth(post("/api/trips/" + tripId + "/hire"), customer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"driverId\":" + driverId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.driver.phone").value(nullValue()));

        // The driver sees the request (with the customer's name but not number).
        mvc.perform(auth(get("/api/trips/assigned"), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.content[0].customer.fullName").value("Test Customer"))
                .andExpect(jsonPath("$.content[0].customer.phone").value(nullValue()));

        // Accept -> CONFIRMED, both phone numbers revealed.
        mvc.perform(auth(post("/api/trips/" + tripId + "/accept"), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.customer.phone").value("9876543210"));
        mvc.perform(auth(get("/api/trips/" + tripId), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.phone").value("9123456789"));

        // Complete.
        mvc.perform(auth(post("/api/trips/" + tripId + "/complete"), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        // A finished trip cannot be cancelled.
        mvc.perform(auth(post("/api/trips/" + tripId + "/cancel"), customer))
                .andExpect(status().isConflict());
    }

    @Test
    void driverDeclineReturnsTripToTheOpenPool() throws Exception {
        String city = uniqueCity();
        String customer = registerCustomer(uniqueEmail("c"), "9876543210", city);
        String driver = registerDriver(uniqueEmail("d"), "9123456789", city, true, true);
        long tripId = createTrip(customer, city, "ONE_WAY", "MANUAL");
        long driverId = firstDriverId(customer, city);

        mvc.perform(auth(post("/api/trips/" + tripId + "/hire"), customer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"driverId\":" + driverId + "}"))
                .andExpect(status().isOk());
        mvc.perform(auth(post("/api/trips/" + tripId + "/decline"), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.driver").value(nullValue()));
    }

    @Test
    void openTripIsVisibleToLocalDriversAndCanBeClaimedOnlyOnce() throws Exception {
        String city = uniqueCity();
        String customer = registerCustomer(uniqueEmail("c"), "9876543210", city);
        String driverA = registerDriver(uniqueEmail("a"), "9123456781", city, true, true);
        String driverB = registerDriver(uniqueEmail("b"), "9123456782", city, true, true);
        String otherCityDriver = registerDriver(uniqueEmail("o"), "9123456783", uniqueCity(), true, true);
        long tripId = createTrip(customer, city, "ONE_WAY", "MANUAL");

        mvc.perform(auth(get("/api/trips/open"), driverA))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(tripId));
        mvc.perform(auth(get("/api/trips/open"), otherCityDriver))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mvc.perform(auth(post("/api/trips/" + tripId + "/accept"), driverA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(auth(post("/api/trips/" + tripId + "/accept"), driverB))
                .andExpect(status().isConflict());
        // Driver B is not part of this trip any more: it must look like it does not exist.
        mvc.perform(auth(get("/api/trips/" + tripId), driverB)).andExpect(status().isNotFound());
    }

    @Test
    void customerCannotHireADriverWhoCannotDriveTheCarsGearbox() throws Exception {
        String city = uniqueCity();
        String customer = registerCustomer(uniqueEmail("c"), "9876543210", city);
        registerDriver(uniqueEmail("d"), "9123456789", city, true, false); // manual only
        long tripId = createTrip(customer, city, "ONE_WAY", "AUTOMATIC");
        long driverId = firstDriverId(customer, city);

        mvc.perform(auth(post("/api/trips/" + tripId + "/hire"), customer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"driverId\":" + driverId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("automatic")));
    }

    @Test
    void otherCustomersCannotSeeOrChangeSomeoneElsesTrip() throws Exception {
        String city = uniqueCity();
        String owner = registerCustomer(uniqueEmail("owner"), "9876543210", city);
        String stranger = registerCustomer(uniqueEmail("stranger"), "9876543211", city);
        long tripId = createTrip(owner, city, "ONE_WAY", "MANUAL");

        mvc.perform(auth(get("/api/trips/" + tripId), stranger)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/trips/" + tripId + "/cancel"), stranger)).andExpect(status().isNotFound());
    }

    @Test
    void tripValidationRules() throws Exception {
        String city = uniqueCity();
        String customer = registerCustomer(uniqueEmail("c"), "9876543210", city);
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);

        // Round trip without a return time.
        mvc.perform(auth(post("/api/trips"), customer).contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(city, "ROUND_TRIP", "MANUAL", start, null)))
                .andExpect(status().isBadRequest());
        // Return before start.
        mvc.perform(auth(post("/api/trips"), customer).contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(city, "ROUND_TRIP", "MANUAL", start, start.minus(1, ChronoUnit.HOURS))))
                .andExpect(status().isBadRequest());
        // Start in the past.
        mvc.perform(auth(post("/api/trips"), customer).contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(city, "ONE_WAY", "MANUAL", Instant.now().minus(1, ChronoUnit.DAYS), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startAt").exists());
    }

    // ---- helpers -----------------------------------------------------------------------------------

    private static String uniqueEmail(String prefix) {
        return prefix + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@example.com";
    }

    private static String uniqueCity() {
        return "Testville" + SEQ.incrementAndGet() + "x" + System.nanoTime();
    }

    private static MockHttpServletRequestBuilder postJson(String url, String json) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private String registerCustomer(String email, String phone, String city) throws Exception {
        String body = mvc.perform(postJson("/api/auth/register/customer", customerJson(email, phone, city)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String registerDriver(String email, String phone, String city, boolean manual, boolean automatic)
            throws Exception {
        String body = mvc.perform(postJson("/api/auth/register/driver",
                        driverJson(email, phone, city, manual, automatic)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private long createTrip(String customerToken, String city, String type, String transmission) throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant ret = "ROUND_TRIP".equals(type) ? start.plus(3, ChronoUnit.DAYS) : null;
        String body = mvc.perform(auth(post("/api/trips"), customerToken).contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(city, type, transmission, start, ret)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long firstDriverId(String customerToken, String city) throws Exception {
        String body = mvc.perform(auth(get("/api/drivers").param("city", city), customerToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.content[0].id")).longValue();
    }

    private static String customerJson(String email, String phone, String city) {
        return """
                {"email":"%s","password":"Password123","fullName":"Test Customer","phone":"%s",
                 "profile":{"address":"1 Main Street","city":"%s","state":"Karnataka"}}"""
                .formatted(email, phone, city);
    }

    private static String driverJson(String email, String phone, String city, boolean manual, boolean automatic) {
        return """
                {"email":"%s","password":"Password123","fullName":"Test Driver","phone":"%s",
                 "profile":{"address":"2 Side Street","city":"%s","state":"Karnataka","yearsOfExperience":7,
                  "licenseNumber":"KA-1234567","licenseExpiry":"%s","drivesManual":%s,"drivesAutomatic":%s,
                  "languages":["English","Kannada"],"bio":"Safe and punctual.","dailyRate":1200.00}}"""
                .formatted(email, phone, city, LocalDate.now().plusYears(3), manual, automatic);
    }

    private static String tripJson(String city, String type, String transmission, Instant start, Instant ret) {
        return """
                {"pickupCity":"%s","pickupLocation":"Bus stand","destination":"Goa","tripType":"%s",
                 "startAt":"%s","returnAt":%s,"carMake":"Maruti","carModel":"Swift","carType":"HATCHBACK",
                 "transmission":"%s","notes":"Two bags"}"""
                .formatted(city, type, start, ret == null ? "null" : "\"" + ret + "\"", transmission);
    }
}
