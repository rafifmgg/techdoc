package com.ocmsintranet.cronservice.testing.agencies.mha;

import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.helpers.MhaNricDownloadHelper;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.offencenoticeaddress.OffenceNoticeAddress;
import com.ocmsintranet.cronservice.crud.ocmsizdb.offencenoticeaddress.OffenceNoticeAddressService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.services.MhaNricDownloadService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaCallbackService;
import com.ocmsintranet.cronservice.testing.agencies.mha.models.TestStepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/test/mha")
@Profile({"local", "sit", "dev", "uat"})
public class MhaNricTestController {

  private final TableQueryService tableQueryService;
  private final MhaNricDownloadHelper mhaNricDownloadHelper;
  private final OffenceNoticeAddressService offenceNoticeAddressService;
  private final MhaNricDownloadService mhaNricDownloadService;
  private final MhaCallbackService mhaCallbackService;
  private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public MhaNricTestController(TableQueryService tableQueryService,
                               MhaNricDownloadHelper mhaNricDownloadHelper,
                               OffenceNoticeAddressService offenceNoticeAddressService,
                               MhaNricDownloadService mhaNricDownloadService,
                               MhaCallbackService mhaCallbackService) {
    this.tableQueryService = tableQueryService;
    this.mhaNricDownloadHelper = mhaNricDownloadHelper;
    this.offenceNoticeAddressService = offenceNoticeAddressService;
    this.mhaNricDownloadService = mhaNricDownloadService;
    this.mhaCallbackService = mhaCallbackService;
  }

  @DeleteMapping("/cleanup/uin/{uin}")
  public ResponseEntity<Map<String, Object>> cleanupByUin(@PathVariable String uin) {
    Map<String, Object> res = new HashMap<>();
    try {
      List<String> noticeNos = getNoticeNosByUin(uin);

      int deletedSusp = 0;
      for (String n : noticeNos) {
        Map<String, Object> f = new HashMap<>();
        f.put("noticeNo", n);
        try { deletedSusp += tableQueryService.delete("ocms_suspended_notice", f); } catch (Exception e) { log.warn("suspended del {}: {}", n, e.getMessage()); }
      }

      Map<String, Object> ownerFilter = new HashMap<>();
      ownerFilter.put("idNo", uin);
      ownerFilter.put("idType", "N");
      int deletedOwner = 0;
      try { deletedOwner = tableQueryService.delete("ocms_offence_notice_owner_driver", ownerFilter); } catch (Exception e) { log.warn("owner del {}: {}", uin, e.getMessage()); }

      int deletedDetail = 0, deletedVon = 0;
      for (String n : noticeNos) {
        Map<String, Object> f = new HashMap<>(); f.put("noticeNo", n);
        try { deletedDetail += tableQueryService.delete("ocms_offence_notice_detail", f); } catch (Exception e) { log.warn("detail del {}: {}", n, e.getMessage()); }
        try { deletedVon += tableQueryService.delete("ocms_valid_offence_notice", f); } catch (Exception e) { log.warn("von del {}: {}", n, e.getMessage()); }
      }

      res.put("success", true);
      res.put("uin", uin);
      res.put("deletedSuspended", deletedSusp);
      res.put("deletedOwner", deletedOwner);
      res.put("deletedDetail", deletedDetail);
      res.put("deletedVon", deletedVon);
      return ResponseEntity.ok(res);
    } catch (Exception e) {
      log.error("cleanup error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  @PostMapping("/setup/notice-owner-bundle")
  public ResponseEntity<Map<String, Object>> setupNoticeOwnerBundle(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());

      if (cleanupFirst) cleanupByUin(uin);

      int vonCreated = 0, vonUpdated = 0, ownerCreated = 0, ownerUpdated = 0;
      for (Map<String, Object> n : setupNotices) {
        String noticeNo = (String) n.get("noticeNo");
        String offenceDateTime = (String) n.get("offenceDateTime");
        if (noticeNo == null || offenceDateTime == null) continue;

        Map<String, Object> vonFilter = new HashMap<>(); vonFilter.put("noticeNo", noticeNo);
        List<Map<String, Object>> existingVon = tableQueryService.query("ocms_valid_offence_notice", vonFilter);
        if (existingVon != null && !existingVon.isEmpty()) {
          Map<String, Object> u = new HashMap<>();
          u.put("noticeDateAndTime", LocalDateTime.parse(offenceDateTime, DATETIME_FORMAT));
          u.put("modDate", LocalDateTime.now()); u.put("modUserId", "TEST_USER");
          tableQueryService.patch("ocms_valid_offence_notice", vonFilter, u); vonUpdated++;
        } else {
          Map<String, Object> f = new HashMap<>();
          f.put("noticeNo", noticeNo);
          f.put("noticeDateAndTime", LocalDateTime.parse(offenceDateTime, DATETIME_FORMAT));
          f.put("vehicleNo", "TEST1234");
          f.put("nextProcessingStage", "ROV"); f.put("lastProcessingStage", "NPA");
          f.put("nextProcessingDate", LocalDateTime.now().plusDays(1)); f.put("lastProcessingDate", LocalDateTime.now());
          f.put("creDate", LocalDateTime.now()); f.put("creUserId", "TEST_USER");
          f.put("modDate", LocalDateTime.now()); f.put("modUserId", "TEST_USER");
          f.put("vehicleCategory", "S"); f.put("offenceNoticeType", "O"); f.put("subsystemLabel", SystemConstant.Subsystem.OCMS_CODE);
          f.put("compositionAmount", new BigDecimal("150.00")); f.put("computerRuleCode", 203); f.put("wardenNo", "W001");
          tableQueryService.post("ocms_valid_offence_notice", f); vonCreated++;
        }

        Map<String, Object> ownerFilter = new HashMap<>();
        ownerFilter.put("noticeNo", noticeNo); ownerFilter.put("idNo", uin); ownerFilter.put("idType", "N");
        List<Map<String, Object>> existingOwner = tableQueryService.query("ocms_offence_notice_owner_driver", ownerFilter);
        Map<String, Object> of = new HashMap<>();
        of.put("noticeNo", noticeNo); of.put("idNo", uin); of.put("idType", "N"); of.put("ownerDriverIndicator", "O");
        of.put("creDate", LocalDateTime.now()); of.put("creUserId", "TEST_USER"); of.put("modDate", LocalDateTime.now()); of.put("modUserId", "TEST_USER");
        if (existingOwner != null && !existingOwner.isEmpty()) { tableQueryService.patch("ocms_offence_notice_owner_driver", ownerFilter, of); ownerUpdated++; }
        else { tableQueryService.post("ocms_offence_notice_owner_driver", of); ownerCreated++; }
      }

      return ResponseEntity.ok(Map.of(
          "success", true,
          "uin", req.get("uin"),
          "noticeCount", setupNotices.size(),
          "vonCreated", vonCreated,
          "vonUpdated", vonUpdated,
          "ownerCreated", ownerCreated,
          "ownerUpdated", ownerUpdated
      ));
    } catch (Exception e) {
      log.error("setup error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  @PostMapping("/run-apply")
  public ResponseEntity<Map<String, Object>> runApply(@RequestBody Map<String, Object> req) {
    try {
      @SuppressWarnings("unchecked") List<Map<String, Object>> records = (List<Map<String, Object>>) req.getOrDefault("records", Collections.emptyList());
      @SuppressWarnings("unchecked") List<Map<String, String>> exceptions = (List<Map<String, String>>) req.getOrDefault("exceptions", Collections.emptyList());

      int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(records, exceptions);

      Set<String> uins = new HashSet<>();
      for (Map<String, Object> r : records) {
        Object u = r.get("uin"); if (u instanceof String) uins.add((String) u);
      }

      List<Map<String, Object>> von = new ArrayList<>();
      List<Map<String, Object>> suspended = new ArrayList<>();
      for (String uin : uins) {
        for (String n : getNoticeNosByUin(uin)) {
          Map<String, Object> f = new HashMap<>(); f.put("noticeNo", n);
          List<Map<String, Object>> v = tableQueryService.query("ocms_valid_offence_notice", f); if (v != null) von.addAll(v);
          List<Map<String, Object>> s = tableQueryService.query("ocms_suspended_notice", f); if (s != null) suspended.addAll(s);
        }
      }

      return ResponseEntity.ok(Map.of(
          "success", true,
          "updatedCount", updatedCount,
          "von", von,
          "suspended", suspended
      ));
    } catch (Exception e) {
      log.error("run-apply error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  @GetMapping("/verify/uin/{uin}")
  public ResponseEntity<Map<String, Object>> verifyByUin(@PathVariable String uin) {
    try {
      List<String> noticeNos = getNoticeNosByUin(uin);
      List<Map<String, Object>> von = new ArrayList<>();
      List<Map<String, Object>> suspended = new ArrayList<>();
      for (String n : noticeNos) {
        Map<String, Object> f = new HashMap<>(); f.put("noticeNo", n);
        List<Map<String, Object>> v = tableQueryService.query("ocms_valid_offence_notice", f); if (v != null) von.addAll(v);
        List<Map<String, Object>> s = tableQueryService.query("ocms_suspended_notice", f); if (s != null) suspended.addAll(s);
      }
      return ResponseEntity.ok(Map.of(
          "success", true,
          "uin", uin,
          "noticeNos", noticeNos,
          "von", von,
          "suspended", suspended
      ));
    } catch (Exception e) {
      log.error("verify error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  // ================================
  // Scenario Endpoints (MHA Download)
  // ================================
  /**
   * POST /test/mha/mha-download-ps-rip
   * Purpose: Seed notices and simulate PS-RIP (lifeStatus='D' + DateOfDeath after offence).
   * Example payload:
   * <pre>
   * {
   *   "uin": "S1234567A",
   *   "cleanupFirst": true,
   *   "setupNotices": [
   *     { "noticeNo": "N001", "offenceDateTime": "2024-05-15 10:00:00" }
   *   ],
   *   "name": "TEST_PS_RIP" // optional
   * }
   * </pre>
   */
  @PostMapping("/mha-download-ps-rip")
  public ResponseEntity<Map<String, Object>> scenarioPsRip(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      }
      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());
      if (setupNotices.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "setupNotices is required"));
      }

      // Seed notices and owner rows
      Map<String, Object> seedReq = new HashMap<>();
      seedReq.put("uin", uin);
      seedReq.put("cleanupFirst", cleanupFirst);
      seedReq.put("setupNotices", setupNotices);
      setupNoticeOwnerBundle(seedReq);

      // Compute DoD after the latest offenceDateTime to force PS-RIP
      LocalDateTime maxOffence = null;
      for (Map<String, Object> n : setupNotices) {
        String dt = (String) n.get("offenceDateTime");
        if (dt != null) {
          LocalDateTime odt = LocalDateTime.parse(dt, DATETIME_FORMAT);
          if (maxOffence == null || odt.isAfter(maxOffence)) maxOffence = odt;
        }
      }
      if (maxOffence == null) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "invalid offenceDateTime in setupNotices"));
      }
      String dodYmd = maxOffence.plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      // Build records (lifeStatus D + DoD triggers PS updates)
      Map<String, Object> rec = new HashMap<>();
      rec.put("uin", uin);
      rec.put("name", req.getOrDefault("name", "TEST_PS_RIP"));
      rec.put("lifeStatus", "D");
      rec.put("dateOfDeath", dodYmd);
      List<Map<String, Object>> records = List.of(rec);
      List<Map<String, String>> exceptions = Collections.emptyList();

      int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(records, exceptions);

      ResponseEntity<Map<String, Object>> verify = verifyByUin(uin);
      Map<String, Object> body = new HashMap<>(verify.getBody() != null ? verify.getBody() : Map.of());
      body.put("success", true);
      body.put("scenario", "PS-RIP");
      body.put("updatedCount", updatedCount);
      body.put("uin", uin);
      body.put("dateOfDeathYmd", dodYmd);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("scenarioPsRip error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  /**
   * POST /test/mha/mha-download-ts-nro-exception
   * Purpose: Seed notices and simulate TS-NRO via exception report.
   * Example payload:
   * <pre>
   * {
   *   "uin": "S1234567A",
   *   "cleanupFirst": true,
   *   "setupNotices": [
   *     { "noticeNo": "N002", "offenceDateTime": "2024-05-15 10:00:00" }
   *   ],
   *   "exceptionStatus": "ADDRESS_INVALID" // optional override
   * }
   * </pre>
   */
  @PostMapping("/mha-download-ts-nro-exception")
  public ResponseEntity<Map<String, Object>> scenarioTsNroException(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      }
      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());
      if (setupNotices.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "setupNotices is required"));
      }

      // Seed notices and owner rows
      Map<String, Object> seedReq = new HashMap<>();
      seedReq.put("uin", uin);
      seedReq.put("cleanupFirst", cleanupFirst);
      seedReq.put("setupNotices", setupNotices);
      setupNoticeOwnerBundle(seedReq);

      // Build exceptions list to trigger TS-NRO path
      Map<String, String> ex = new HashMap<>();
      ex.put("idNumber", uin);
      ex.put("exceptionStatus", String.valueOf(req.getOrDefault("exceptionStatus", "ADDRESS_INVALID")));
      List<Map<String, String>> exceptions = List.of(ex);

      int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(Collections.emptyList(), exceptions);

      ResponseEntity<Map<String, Object>> verify = verifyByUin(uin);
      Map<String, Object> body = new HashMap<>(verify.getBody() != null ? verify.getBody() : Map.of());
      body.put("success", true);
      body.put("scenario", "TS-NRO (exception)");
      body.put("updatedCount", updatedCount);
      body.put("uin", uin);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("scenarioTsNroException error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  /**
   * POST /test/mha/mha-download-ps-rp2
   * Purpose: Seed notices and simulate PS-RP2 (lifeStatus='D' + DateOfDeath before offence).
   * Example payload:
   * <pre>
   * {
   *   "uin": "S1234567A",
   *   "cleanupFirst": true,
   *   "setupNotices": [
   *     { "noticeNo": "N003", "offenceDateTime": "2024-05-15 10:00:00" }
   *   ],
   *   "name": "TEST_PS_RP2" // optional
   * }
   * </pre>
   */
  @PostMapping("/mha-download-ps-rp2")
  public ResponseEntity<Map<String, Object>> scenarioPsRp2(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      }
      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());
      if (setupNotices.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "setupNotices is required"));
      }

      Map<String, Object> seedReq = new HashMap<>();
      seedReq.put("uin", uin);
      seedReq.put("cleanupFirst", cleanupFirst);
      seedReq.put("setupNotices", setupNotices);
      setupNoticeOwnerBundle(seedReq);

      // Compute DoD before the earliest offenceDateTime to force PS-RP2
      LocalDateTime minOffence = null;
      for (Map<String, Object> n : setupNotices) {
        String dt = (String) n.get("offenceDateTime");
        if (dt != null) {
          LocalDateTime odt = LocalDateTime.parse(dt, DATETIME_FORMAT);
          if (minOffence == null || odt.isBefore(minOffence)) minOffence = odt;
        }
      }
      if (minOffence == null) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "invalid offenceDateTime in setupNotices"));
      }
      String dodYmd = minOffence.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      Map<String, Object> rec = new HashMap<>();
      rec.put("uin", uin);
      rec.put("name", req.getOrDefault("name", "TEST_PS_RP2"));
      rec.put("lifeStatus", "D");
      rec.put("dateOfDeath", dodYmd);
      int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(List.of(rec), Collections.emptyList());

      ResponseEntity<Map<String, Object>> verify = verifyByUin(uin);
      Map<String, Object> body = new HashMap<>(verify.getBody() != null ? verify.getBody() : Map.of());
      body.put("success", true);
      body.put("scenario", "PS-RP2");
      body.put("updatedCount", updatedCount);
      body.put("uin", uin);
      body.put("dateOfDeathYmd", dodYmd);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("scenarioPsRp2 error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  /**
   * POST /test/mha/mha-download-ts-nro-invalid
   * Purpose: Seed notices and simulate TS-NRO via main-record invalid address.
   * Example payload:
   * <pre>
   * {
   *   "uin": "S1234567A",
   *   "cleanupFirst": true,
   *   "setupNotices": [
   *     { "noticeNo": "N004", "offenceDateTime": "2024-05-15 10:00:00" }
   *   ],
   *   "name": "TEST_TS_NRO_INVALID", // optional
   *   "lifeStatus": "A",              // optional
   *   "invalidAddressTag": "Y",       // 'Y' triggers TS-NRO main-record
   *   "streetName": "NA",             // optional
   *   "postalCode": "000000"          // optional
   * }
   * </pre>
   */
  @PostMapping("/mha-download-ts-nro-invalid")
  public ResponseEntity<Map<String, Object>> scenarioTsNroInvalid(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      }
      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());
      if (setupNotices.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "setupNotices is required"));
      }

      Map<String, Object> seedReq = new HashMap<>();
      seedReq.put("uin", uin);
      seedReq.put("cleanupFirst", cleanupFirst);
      seedReq.put("setupNotices", setupNotices);
      setupNoticeOwnerBundle(seedReq);

      // Record with invalid address signals (invalidAddressTag OR streetName==NA & postalCode==000000)
      Map<String, Object> rec = new HashMap<>();
      rec.put("uin", uin);
      rec.put("name", req.getOrDefault("name", "TEST_TS_NRO_INVALID"));
      rec.put("lifeStatus", req.getOrDefault("lifeStatus", "A"));
      rec.put("invalidAddressTag", req.getOrDefault("invalidAddressTag", "Y"));
      rec.put("streetName", req.getOrDefault("streetName", "NA"));
      rec.put("postalCode", req.getOrDefault("postalCode", "000000"));

      int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(List.of(rec), Collections.emptyList());

      ResponseEntity<Map<String, Object>> verify = verifyByUin(uin);
      Map<String, Object> body = new HashMap<>(verify.getBody() != null ? verify.getBody() : Map.of());
      body.put("success", true);
      body.put("scenario", "TS-NRO (invalid address main record)");
      body.put("updatedCount", updatedCount);
      body.put("uin", uin);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("scenarioTsNroInvalid error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  /**
   * POST /test/mha/mha-download-address-upsert
   * Purpose: Seed notices and upsert owner's address (type 'mha_reg').
   * Example payload:
   * <pre>
   * {
   *   "uin": "S1234567A",
   *   "cleanupFirst": true,
   *   "setupNotices": [
   *     { "noticeNo": "N005", "offenceDateTime": "2024-05-15 10:00:00" }
   *   ],
   *   "name": "TEST_ADDR",         // optional
   *   "lifeStatus": "A",           // optional
   *   "blockHouseNo": "10",
   *   "streetName": "JALAN TEST",
   *   "floorNo": "12",
   *   "unitNo": "34",
   *   "buildingName": "BLK-TEST",
   *   "postalCode": "123456",
   *   "addressType": "RES",        // optional
   *   "invalidAddressTag": "",     // optional
   *   "lastChangeAddressDate": "20240515" // YYYYMMDD
   * }
   * </pre>
   */
  @PostMapping("/mha-download-address-upsert")
  public ResponseEntity<Map<String, Object>> scenarioAddressUpsert(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      }
      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());
      if (setupNotices.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "setupNotices is required"));
      }

      Map<String, Object> seedReq = new HashMap<>();
      seedReq.put("uin", uin);
      seedReq.put("cleanupFirst", cleanupFirst);
      seedReq.put("setupNotices", setupNotices);
      setupNoticeOwnerBundle(seedReq);

      // Build record with address fields to upsert OffenceNoticeAddress (type mha_reg)
      Map<String, Object> rec = new HashMap<>();
      rec.put("uin", uin);
      rec.put("name", req.getOrDefault("name", "TEST_ADDR"));
      rec.put("lifeStatus", req.getOrDefault("lifeStatus", "A"));
      rec.put("blockHouseNo", req.getOrDefault("blockHouseNo", "10"));
      rec.put("streetName", req.getOrDefault("streetName", "JALAN TEST"));
      rec.put("floorNo", req.getOrDefault("floorNo", "12"));
      rec.put("unitNo", req.getOrDefault("unitNo", "34"));
      rec.put("buildingName", req.getOrDefault("buildingName", "BLK-TEST"));
      rec.put("postalCode", req.getOrDefault("postalCode", "123456"));
      rec.put("addressType", req.getOrDefault("addressType", "RES"));
      rec.put("invalidAddressTag", req.getOrDefault("invalidAddressTag", ""));
      // Use YYYYMMDD per helper.parseDate
      rec.put("lastChangeAddressDate", req.getOrDefault("lastChangeAddressDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));

      int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(List.of(rec), Collections.emptyList());

      // Collect addresses by notice for verification
      List<String> noticeNos = getNoticeNosByUin(uin);
      List<Map<String, Object>> addresses = new ArrayList<>();
      for (String noticeNo : noticeNos) {
        List<OffenceNoticeAddress> found = offenceNoticeAddressService.findByNoticeNoAndOwnerDriverIndicatorAndTypeOfAddress(noticeNo, "O", "mha_reg");
        for (OffenceNoticeAddress a : found) {
          Map<String, Object> row = new HashMap<>();
          row.put("noticeNo", a.getNoticeNo());
          row.put("ownerDriverIndicator", a.getOwnerDriverIndicator());
          row.put("typeOfAddress", a.getTypeOfAddress());
          row.put("blkHseNo", a.getBlkHseNo());
          row.put("streetName", a.getStreetName());
          row.put("floorNo", a.getFloorNo());
          row.put("unitNo", a.getUnitNo());
          row.put("bldgName", a.getBldgName());
          row.put("postalCode", a.getPostalCode());
          row.put("addressType", a.getAddressType());
          row.put("invalidAddrTag", a.getInvalidAddrTag());
          row.put("effectiveDate", a.getEffectiveDate());
          row.put("processingDateTime", a.getProcessingDateTime());
          addresses.add(row);
        }
      }

      Map<String, Object> body = new HashMap<>();
      body.put("success", true);
      body.put("scenario", "Address Upsert (mha_reg)");
      body.put("updatedCount", updatedCount);
      body.put("uin", uin);
      body.put("addresses", addresses);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("scenarioAddressUpsert error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  /**
   * POST /test/mha/mha-download-owner-update
   * Purpose: Seed notices and update basic owner info.
   * Example payload:
   * <pre>
   * {
   *   "uin": "S1234567A",
   *   "cleanupFirst": true,
   *   "setupNotices": [
   *     { "noticeNo": "N006", "offenceDateTime": "2024-05-15 10:00:00" }
   *   ],
   *   "name": "UPDATED_OWNER",   // optional
   *   "lifeStatus": "A",         // optional
   *   "dateOfBirth": "19900101"  // YYYYMMDD
   * }
   * </pre>
   */
  @PostMapping("/mha-download-owner-update")
  public ResponseEntity<Map<String, Object>> scenarioOwnerUpdate(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      }
      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());
      if (setupNotices.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "setupNotices is required"));
      }

      Map<String, Object> seedReq = new HashMap<>();
      seedReq.put("uin", uin);
      seedReq.put("cleanupFirst", cleanupFirst);
      seedReq.put("setupNotices", setupNotices);
      setupNoticeOwnerBundle(seedReq);

      Map<String, Object> rec = new HashMap<>();
      rec.put("uin", uin);
      rec.put("name", req.getOrDefault("name", "UPDATED_OWNER"));
      rec.put("lifeStatus", req.getOrDefault("lifeStatus", "A"));
      rec.put("dateOfBirth", req.getOrDefault("dateOfBirth", "19900101"));
      int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(List.of(rec), Collections.emptyList());

      // Pull owner_driver rows
      Map<String, Object> f = new HashMap<>();
      f.put("idNo", uin);
      f.put("idType", "N");
      List<Map<String, Object>> ownerRows = tableQueryService.query("ocms_offence_notice_owner_driver", f);

      Map<String, Object> body = new HashMap<>();
      body.put("success", true);
      body.put("scenario", "Owner Update");
      body.put("updatedCount", updatedCount);
      body.put("uin", uin);
      body.put("ownerRows", ownerRows);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("scenarioOwnerUpdate error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  /**
   * POST /test/mha/mha-download-e2e
   * Purpose: Full E2E flow — cleanup+seed -> generate/upload callback -> run job -> verify by UIN.
   * Example payload:
   * <pre>
   * {
   *   "uin": "S1234567A",
   *   "cleanupFirst": true,
   *   "setupNotices": [
   *     { "noticeNo": "N007", "offenceDateTime": "2024-05-15 10:00:00" }
   *   ]
   * }
   * </pre>
   */
  @PostMapping("/mha-download-e2e")
  public ResponseEntity<Map<String, Object>> scenarioMhaDownloadE2E(@RequestBody Map<String, Object> req) {
    try {
      String uin = (String) req.get("uin");
      if (uin == null || uin.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "uin is required"));
      }

      boolean cleanupFirst = (boolean) req.getOrDefault("cleanupFirst", true);
      @SuppressWarnings("unchecked") List<Map<String, Object>> setupNotices = (List<Map<String, Object>>) req.getOrDefault("setupNotices", Collections.emptyList());

      // Cleanup + seeding: gunakan existing endpoint logic untuk konsistensi
      Map<String, Object> setupSummary = null;
      if (!setupNotices.isEmpty()) {
        ResponseEntity<Map<String, Object>> setupRes = setupNoticeOwnerBundle(req);
        setupSummary = setupRes.getBody();
      } else if (cleanupFirst) {
        cleanupByUin(uin);
      }

      // Step 1: Generate & upload output file to SFTP /mhanro/output
      List<TestStepResult> callbackResults = mhaCallbackService.processCallbackTest();

      // Step 2: Trigger MHA NRIC Download job dan tunggu hasil
      boolean jobSuccess = false;
      String jobError = null;
      try {
        jobSuccess = mhaNricDownloadService.executeJob().get();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        jobError = ie.getMessage();
      } catch (ExecutionException ee) {
        jobError = ee.getMessage();
      }

      // Step 3: Verifikasi DB berdasarkan UIN
      ResponseEntity<Map<String, Object>> verify = verifyByUin(uin);
      Map<String, Object> body = new HashMap<>(verify.getBody() != null ? verify.getBody() : Map.of());

      body.put("success", jobSuccess);
      body.put("scenario", "MHA NRIC Download E2E");
      body.put("uin", uin);
      body.put("jobSuccess", jobSuccess);
      if (jobError != null) body.put("jobError", jobError);
      body.put("callbackResults", callbackResults);
      if (setupSummary != null) body.put("setupSummary", setupSummary);

      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("scenarioMhaDownloadE2E error", e);
      return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
    }
  }

  private List<String> getNoticeNosByUin(String uin) {
    Map<String, Object> f = new HashMap<>();
    f.put("idNo", uin); f.put("idType", "N");
    List<Map<String, Object>> rows = tableQueryService.query("ocms_offence_notice_owner_driver", f);
    List<String> noticeNos = new ArrayList<>();
    if (rows != null) {
      for (Map<String, Object> r : rows) {
        Object n = r.get("noticeNo"); if (n instanceof String) noticeNos.add((String) n);
      }
    }
    return noticeNos;
  }
}
