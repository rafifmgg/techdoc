package com.ocmsintranet.cronservice.testing.agencies.lta.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constants class containing vehicle data for testing purposes.
 * Converted from excel-to-json.json
 */
public class VehicleDataConstants {

  /**
   * Vehicle data record structure
   */
  public static class VehicleData {
    private String recordType;
    private String vehicleRegistrationNumber;
    private String chassisNumber;
    private String diplomaticFlag; // Y or N
    private String noticeNumber; // New field
    private String ownerIdType; // Can be String or String
    private String passportPlace; // New field
    private String ownerId;
    private String ownerName;
    private String addressType;
    private String blockHouseNumber; // Can be String or Double
    private String streetName;
    private String floorNumber; // Can be String or Double
    private String unitNumber; // Can be String or Double
    private String buildingName;
    private String postalCode;
    private String makeDescription;
    private String primaryColour;
    private String secondaryColour; // New field
    private String roadTaxExpiryDate;
    private String unladenWeight;
    private String maximumLadenWeight;
    private String effectiveOwnershipDate;
    private String deregistrationDate; // New field
    private String returnErrorCode;
    private String processingDate;
    private String processingTime;
    private String iuObuLabelNumber;
    private String registeredAddressEffectiveDate;
    private String mailingBlockHouse; // New field
    private String mailingStreetName; // New field
    private String mailingFloorNumber; // New field
    private String mailingUnitNumber; // New field
    private String mailingBuildingName; // New field
    private String mailingPostalCode; // New field
    private String mailingAddressEffectiveDate; // New field

    public VehicleData(String recordType, String vehicleRegistrationNumber, String chassisNumber,
                      String diplomaticFlag, String noticeNumber, String ownerIdType, String passportPlace, String ownerId, String ownerName, String addressType,
                      String blockHouseNumber, String streetName, String floorNumber, String unitNumber,
                      String buildingName, String postalCode, String makeDescription, String primaryColour,
                      String secondaryColour, String roadTaxExpiryDate, String unladenWeight, String maximumLadenWeight,
                      String effectiveOwnershipDate, String deregistrationDate, String returnErrorCode, String processingDate,
                      String processingTime, String iuObuLabelNumber, String registeredAddressEffectiveDate,
                      String mailingBlockHouse, String mailingStreetName, String mailingFloorNumber, String mailingUnitNumber,
                      String mailingBuildingName, String mailingPostalCode, String mailingAddressEffectiveDate) {
      this.recordType = recordType;
      this.vehicleRegistrationNumber = vehicleRegistrationNumber;
      this.chassisNumber = chassisNumber;
      this.diplomaticFlag = diplomaticFlag;
      this.noticeNumber = noticeNumber;
      this.ownerIdType = ownerIdType;
      this.passportPlace = passportPlace;
      this.ownerId = ownerId;
      this.ownerName = ownerName;
      this.addressType = addressType;
      this.blockHouseNumber = blockHouseNumber;
      this.streetName = streetName;
      this.floorNumber = floorNumber;
      this.unitNumber = unitNumber;
      this.buildingName = buildingName;
      this.postalCode = postalCode;
      this.makeDescription = makeDescription;
      this.primaryColour = primaryColour;
      this.secondaryColour = secondaryColour;
      this.roadTaxExpiryDate = roadTaxExpiryDate;
      this.unladenWeight = unladenWeight;
      this.maximumLadenWeight = maximumLadenWeight;
      this.effectiveOwnershipDate = effectiveOwnershipDate;
      this.deregistrationDate = deregistrationDate;
      this.returnErrorCode = returnErrorCode;
      this.processingDate = processingDate;
      this.processingTime = processingTime;
      this.iuObuLabelNumber = iuObuLabelNumber;
      this.registeredAddressEffectiveDate = registeredAddressEffectiveDate;
      this.mailingBlockHouse = mailingBlockHouse;
      this.mailingStreetName = mailingStreetName;
      this.mailingFloorNumber = mailingFloorNumber;
      this.mailingUnitNumber = mailingUnitNumber;
      this.mailingBuildingName = mailingBuildingName;
      this.mailingPostalCode = mailingPostalCode;
      this.mailingAddressEffectiveDate = mailingAddressEffectiveDate;
    }

    // Getters
    public String getRecordType() {
      return recordType;
    }

    public String getVehicleRegistrationNumber() {
      return vehicleRegistrationNumber;
    }

    public String getChassisNumber() {
      return chassisNumber;
    }

    public String getDiplomaticFlag() {
      return diplomaticFlag;
    }

    public String getNoticeNumber() {
      return noticeNumber;
    }

    public String getOwnerIdType() {
      return ownerIdType;
    }

    public String getPassportPlace() {
      return passportPlace;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public String getAddressType() {
      return addressType;
    }

    public String getBlockHouseNumber() {
      return blockHouseNumber;
    }

    public String getStreetName() {
      return streetName;
    }

    public String getFloorNumber() {
      return floorNumber;
    }

    public String getUnitNumber() {
      return unitNumber;
    }

    public String getBuildingName() {
      return buildingName;
    }

    public String getPostalCode() {
      return postalCode;
    }

    public String getMakeDescription() {
      return makeDescription;
    }

    public String getPrimaryColour() {
      return primaryColour;
    }

    public String getSecondaryColour() {
      return secondaryColour;
    }

    public String getRoadTaxExpiryDate() {
      return roadTaxExpiryDate;
    }

    public String getUnladenWeight() {
      return unladenWeight;
    }

    public String getMaximumLadenWeight() {
      return maximumLadenWeight;
    }

    public String getEffectiveOwnershipDate() {
      return effectiveOwnershipDate;
    }

    public String getDeregistrationDate() {
      return deregistrationDate;
    }

    public String getReturnErrorCode() {
      return returnErrorCode;
    }

    public String getProcessingDate() {
      return processingDate;
    }

    public String getProcessingTime() {
      return processingTime;
    }

    public String getIuObuLabelNumber() {
      return iuObuLabelNumber;
    }

    public String getRegisteredAddressEffectiveDate() {
      return registeredAddressEffectiveDate;
    }

    public String getMailingBlockHouse() {
      return mailingBlockHouse;
    }

    public String getMailingStreetName() {
      return mailingStreetName;
    }

    public String getMailingFloorNumber() {
      return mailingFloorNumber;
    }

    public String getMailingUnitNumber() {
      return mailingUnitNumber;
    }

    public String getMailingBuildingName() {
      return mailingBuildingName;
    }

    public String getMailingPostalCode() {
      return mailingPostalCode;
    }

    public String getMailingAddressEffectiveDate() {
      return mailingAddressEffectiveDate;
    }
  }

  /**
   * List of all vehicle data records
   */
  public static final List<VehicleData> VEHICLE_DATA = new ArrayList<>(Arrays.asList(
    // DEV Dummy S6161D - Mgg Software - Contact (FIN): 80739116
    new VehicleData(
        "D", "S6161D", "ZAM57XSA2E1123459",
        "N", "", "D", "", "G3032549X", "Mgg Software", "A",
        "11", "Pasir Ris Drive 6", "26", "14",
        "", "567042", "B.M.W.  ", "Orange", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "7577799288", "19980410",
        "", "", "", "", "", "", ""
    ),
    // DEV Dummy S0007G - Mgg Software Sg - Contact (FIN): 99999999
    new VehicleData(
        "D", "S0007G", "1HGCM82633A004356",
        "N", "", "D", "", "F1175231N", "Mgg Software Sg", "C",
        "11", "Sunset Way", "57", "1332",
        "", "459210", "M.G.  ", "Silver", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "713574631", "20110318",
        "", "", "", "", "", "", ""
    ),
    // DEV Dummy S0007G - Mgg Software - Contact (NRIC): 80739117
    new VehicleData(
        "D", "S5905G", "1HGCM82633A004356",
        "N", "", "1", "", "S6239817E", "Mgg Software", "C",
        "11", "Sunset Way", "57", "1332",
        "", "459210", "M.G.", "Silver Blue", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "713574631", "20110318",
        "", "", "", "", "", "", ""
    ),
    // DEV Dummy S5558A - Mgg Software Id - Email (UEN): deanwinchester@mailinator.com
    new VehicleData(
        "D", "S5558A", "5TDZK3EH6ES123460",
        "N", "", "8", "", "C18000931H", "Mgg Software Id", "X",
        "43", "Biopolis Drive", "57", "42",
        "", "407982", "Kia  ", "Blue", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "7521213118", "20150915",
        "", "", "", "", "", "", ""
    ),
    // EM8667X - Liang Wei Chen
    new VehicleData(
        "D", "EM8667X", "ZAM57XSA2E1123459",
        "N", "", "D", "", "F6120458W", "Liang Wei Chen", "A",
        "11", "Pasir Ris Drive 6", "26", "14",
        "", "567042", "B.M.W.  ", "Orange", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "7577799288", "19980410",
        "", "", "", "", "", "", ""
    ),
    // FBA403S - Asif Rajan
    new VehicleData(
        "D", "FBA403S", "1HGCM82633A004356",
        "N", "", "1", "", "S8234567D", "Asif Rajan", "C",
        "11", "Sunset Way", "57", "1332",
        "", "459210", "M.G.  ", "Silver", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "713574631", "20110318",
        "", "", "", "", "", "", ""
    ),
    // FBA7225T - Wong Siao Yee
    new VehicleData(
        "D", "FBA7225T", "JF1GH6B69BH654324",
        "N", "", "1", "", "S7409134B", "Wong Siao Yee", "C",
        "5", "Eastwood Road", "53", "92",
        "", "618209", "Subaru  ", "Gold", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "8767811388", "20131129",
        "", "", "", "", "", "", ""
    ),
    // FBD870K - Jean-Luc Moreau (Diplomat private car)
    new VehicleData(
        "D", "FBD870K", "1FTSW21P07EB12349",
        "Y", "", "D", "", "F0012043W", "Jean-Luc Moreau", "A",
        "90", "Chai Chee Road", "23", "78B",
        "Riviera Heights", "429830", "Mercedes-Benz  ", "Multicolour", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "8996222427", "20030509",
        "", "", "", "", "", "", ""
    ),
    // GBK1444P - House of Faith
    new VehicleData(
        "D", "GBK1444P", "5TDZK3EH6ES123460",
        "N", "", "8", "", "S82SS0030J", "House of Faith", "X",
        "43", "Biopolis Drive", "57", "42",
        "", "407982", "Kia  ", "Blue", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "7521213118", "20150915",
        "", "", "", "", "", "", ""
    ),
    // GBK6214D - Pinnacle Growth Ventures LP
    new VehicleData(
        "D", "GBK6214D", "5J6RM4H57EL654323",
        "N", "", "C", "", "T18LP0001A", "Pinnacle Growth Ventures LP", "X",
        "277", "Jurong Pier Road", "40", "23A",
        "", "556039", "Mitsubishi  ", "White", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "1043288149", "20191116",
        "", "", "", "", "", "", ""
    ),
    // GBK927U - ARKEN CONSULTING GROUP
    new VehicleData(
        "D", "GBK927U", "WDBJF65JYXA123456",
        "N", "", "9", "", "T07PF0897A", "ARKEN CONSULTING GROUP", "X",
        "32", "Kitchener Road", "73", "86",
        "", "619348", "Hyundai  ", "Yellow", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "1844056743", "20030509",
        "", "", "", "", "", "", ""
    ),
    // GBL5557R - Sanjay Kumar
    new VehicleData(
        "D", "GBL5557R", "5NPEB4AC6DH123460",
        "N", "", "1", "", "T0982274C", "Sanjay Kumar", "A",
        "130", "Bedok Reservoir Road", "79", "1480",
        "", "408039", "M.G.  ", "White", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "1043259055", "20090704",
        "", "", "", "", "", "", ""
    ),
    // GBL7820S - SEAFRONT TOWN COUNCIL
    new VehicleData(
        "D", "GBL7820S", "5TDZK3EH6ES123459",
        "N", "", "5", "", "T06TC0011A", "SEAFRONT TOWN COUNCIL", "X",
        "619", "Pasir Ris Drive 3", "69", "3A",
        "", "612302", "BYD  ", "Navy", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "2203633822", "20080225",
        "", "", "", "", "", "", ""
    ),
    // GBM2596S - BlueHarbor Logistics Pte. Ltd.
    new VehicleData(
        "D", "GBM2596S", "KMHDU46D77U654321",
        "N", "", "7", "", "201615971R", "BlueHarbor Logistics Pte. Ltd.", "X",
        "208", "Kaki Bukit Avenue 1", "9", "64A",
        "", "522908", "Subaru  ", "Grey", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "5224662647", "20050607",
        "", "", "", "", "", "", ""
    ),
    // S1055CD - Malaysian Embassy (Diplomat embassy car)
    new VehicleData(
        "D", "S1055CD", "3VWDP7AJ0DM654325",
        "Y", "", "9", "", "S90DP0053H", "Malaysian Embassy", "X",
        "37", "Lorong Chencharu", "60", "48",
        "", "689301", "M.G.  ", "Beige", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "4517804847", "20160718",
        "", "", "", "", "", "", ""
    ),
    // SFU5501G - Mirah Mastura
    new VehicleData(
        "D", "SFU5501G", "JTDKN3DU0E1234569",
        "N", "", "1", "", "S5072796C", "Mirah Mastura", "B",
        "121", "Meyer Road", "34", "74",
        "Marina Belle", "538240", "Kia  ", "Grey", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "8142380299", "20051023",
        "", "", "", "", "", "", ""
    ),
    // SFW6733L - Ngee Yuk Kim
    new VehicleData(
        "D", "SFW6733L", "2HGFG12628H654327",
        "N", "", "2", "", "900101-14-5678", "Ngee Yuk Kim", "A",
        "89B", "Jalan Setia Impian", "11", "88",
        "Vista Harmoni", "602134", "XPENG  ", "Purple", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "7059434744", "20131129",
        "", "", "", "", "", "", ""
    ),
    // SFW8207B - Sylvia Gan
    new VehicleData(
        "D", "SFW8207B", "KMHDH4AE1CU123459",
        "N", "", "1", "", "S9563105C", "Sylvia Gan", "C",
        "12", "Paya Lebar Crescent", "63", "78B",
        "", "530923", "M.G.  ", "Multicolour", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "4457853685", "19910621",
        "", "", "", "", "", "", ""
    ),
    // SFX652X - Suresh Kumar
    new VehicleData(
        "D", "SFX652X", "1G1JC5444R7252372",
        "N", "", "1", "", "S9458701H", "Suresh Kumar", "C",
        "74", "Nassim Road", "63", "2C",
        "", "608971", "Kia  ", "Bronze", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "9848410548", "20160623",
        "", "", "", "", "", "", ""
    ),
    // SFY1435U - Beckham Li Zheng De
    new VehicleData(
        "D", "SFY1435U", "2C3KA63H86H654321",
        "N", "", "1", "", "S7341287J", "Beckham Li Zheng De", "C",
        "97", "Casuarina Road", "51", "53",
        "", "346720", "B.M.W.  ", "Orange", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "5990814512", "20170901",
        "", "", "", "", "", "", ""
    ),
    // SGT4578K - Faridah Binte Ismail
    new VehicleData(
        "D", "SGT4578K", "KL1TD56696B123461",
        "N", "", "1", "", "S5738294Z", "Faridah Binte Ismail", "B",
        "583", "Balmoral Road", "16", "1923",
        "Serene Vibe Residences", "398201", "M.G.  ", "Beige", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "1121083850", "20050607",
        "", "", "", "", "", "", ""
    ),
    // SGY9983P - Alfan Yusof
    new VehicleData(
        "D", "SGY9983P", "KMHDH4AE1CU123456",
        "N", "", "1", "", "T9661609G", "Alfan Yusof", "A",
        "139", "Tampines Street 83", "07", "2C",
        "", "545091", "MINI  ", "Bronze", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "5141058646", "19910621",
        "", "", "", "", "", "", ""
    ),
    // SDK4736S - Ji Li Ren - Passport
    new VehicleData(
       "D", "SDK4736S", "5J6RM4H57EL654325",
       "", "", "3", "CHN", "E12345678", "Ji Li Ren", "A",
       "88", "Nanjing East Road", "12", "2C",
       "Harmony Garden Tower", "200001", "B.M.W.", "Bronze", "",
       "20251231", "0001685", "0002123",
       "20231001", "", "0", "20250610",
       "1001", "1962384201", "20081130",
       "", "", "", "", "", "", ""
    ),
    // SGJ4446K - Yap Ping Hwee - edit ownerId 920713-05-1234 -> 123456789012
    new VehicleData(
      "D", "SGJ4446K", "1FTSW21P07EB12350",
      "", "", "2", "", "123456789012", "Yap Ping Hwee", "A",
      "25", "Jalan Seri Suria 8", "06", "1930",
      "D'Menara Residensi", "487903", "M.G.", "Pink", "",
      "20251231", "0001685", "0002123",
      "20231001", "", "0",
      "20250610", "1001", "9328728559", "20160718",
      "", "", "", "", "", "", ""
    ),
    // SGJ8621U - Karthik Rajan - edit ownerId 780429-08-9123 -> 123456789013, ownerIdType = 2 -> 3
    new VehicleData(
      "D", "SGJ8621U", "2HKYF184X3H567895",
      "", "", "3", "", "123456789013", "Karthik Rajan", "A",
      "12A", "Jalan Damai Raya", "12", "48",
      "Seri Mutiara Court", "541290", "Mercedes-Benz  ", "Beige", "",
      "20251231", "0001685", "0002123",
      "20231001", "", "0",
      "20250610", "1001", "2966658337", "20000923",
      "", "", "", "", "", "", ""
    ),
    // SML1783U - Roland Moss - edit ownerIdType = 3
    new VehicleData(
      "D", "SML1783U", "KL1TD56696B123462",
      "", "", "3", "AUS", "N1234567", "Roland Moss", "A",
      "56A", "Victoria Parade", "05", "8C",
      "Coral View Residences", "3002", "Lexus  ", "Black", "",
      "20251231", "0001685", "0002123", "20231001", "", "0",
      "20250610", "1001", "1220686183", "20040820",
      "", "", "", "", "", "", ""
    ),
    // ------------------ ERROR CODE : 2 3 ------------------
    // GB7227Z - Serenity Living Solutions Pte Ltd
    new VehicleData(
        "D", "GB7227Z", "3VWFE21C04M000004",
        "N", "", "7", "", "200301690G", "Serenity Living Solutions Pte Ltd", "X",
        "145", "Changi Business Park Avenue 1", "30", "63",
        "Quantum Industrial Park", "339865", "Audi  ", "Pink", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "9568005126", "20081130",
        "", "", "", "", "", "", ""
    ),
    // S7588A - EC1
    new VehicleData(
        "D", "S7588A", "",
        "", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "1", "",
        "", "", "",
        "", "", "", "", "", "", ""
    ),
    // S9537J - EC1
    new VehicleData(
        "D", "S9537J", "",
        "", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "1", "",
        "", "", "",
        "", "", "", "", "", "", ""
    ),
    // S9666U - EC1
    new VehicleData(
        "D", "S9666U", "",
        "", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "1", "",
        "", "", "",
        "", "", "", "", "", "", ""
    ),
    // S3508TE - EC1
    new VehicleData(
        "D", "S3508TE", "",
        "", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "1", "",
        "", "", "",
        "", "", "", "", "", "", ""
    ),
    // GBD9233R - Harmony Healthcare Group Pte Ltd
    new VehicleData(
        "D", "GBD9233R", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // GBE6467Y - Pinnacle Engineering Pte Ltd
    new VehicleData(
        "D", "GBE6467Y", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // SBB2003U - Evergreen Logistics Pte Ltd
    new VehicleData(
        "D", "SBB2003U", "3VWFE21C04M000001",
        "N", "", "B", "", "T16LL1863K", "159 AUTOHUB RENTALS LLP", "X",
        "208", "Ubi Techpark", "85", "1543",
        "", "732081", "Honda  ", "Multicolour", "",
        "20251231", "0001685", "0002123",
        "20231001", "20250531", "3", "20250610",
        "1001", "7293262323", "20050607",
        "", "", "", "", "", "", ""
    ),
    // SKL7219Y - Michael Wong
    new VehicleData(
        "D", "SKL7219Y", "1B7GG26X2XS123457",
        "N", "", "1", "", "S0410379C", "Fang Wei Le", "A",
        "418", "Bedok North Street 3", "32", "85",
        "", "530912", "Honda  ", "Green", "",
        "20251231", "0001685", "0002123",
        "20231001", "20250531", "3", "20250610",
        "1001", "9486835574", "19980410",
        "", "", "", "", "", "", ""
    ),
    // SKR3744S - Priya Sharma
    new VehicleData(
        "D", "SKR3744S", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // SKR9003H - Ahmad Bin Abdullah
    new VehicleData(
        "D", "SKR9003H", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // SKX6913U - Rachel Tan
    new VehicleData(
        "D", "SKX6913U", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // SKY7229A - David Lim
    new VehicleData(
        "D", "SKY7229A", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // SLQ842E - Lakshmi Nair
    new VehicleData(
        "D", "SLQ842E", "2HKYF184X3H567892",
        "N", "", "1", "", "S7450757C", "Lionel Linwood Tupper", "C",
        "26", "Frankel Avenue", "54", "2045",
        "", "730482", "Toyota  ", "Purple", "",
        "20251231", "0001685", "0002123",
        "20231001", "20250531", "3", "20250610",
        "1001", "7141093732", "20090805",
        "", "", "", "", "", "", ""
    ),
    // SMC8020U - Jason Tan
    new VehicleData(
        "D", "SMC8020U", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "", "",
        "", "", "",
        "", "", "", "", "", "", ""
    ),
    // SMH8005M - Emily Chen
    new VehicleData(
        "D", "SMH8005M", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "", "",
        "", "", "",
        "", "", "", "", "", "", ""
    ),
    // SMP2021M - Rajesh Kumar
    new VehicleData(
        "D", "SMP2021M", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "", "",
        "", "", "",
        "", "", "", "", "", "", ""
    ),
    // SMQ8201R - Li Wei
    new VehicleData(
        "D", "SMQ8201R", "3VWDP7AJ0DM654323",
        "N", "", "9", "", "T07PF0896E", "NOVA CORPORATE ACCOUNTANTS", "X",
        "216", "Lavender Street", "74", "52",
        "", "670923", "Mercedes-Benz  ", "Green", "",
        "20251231", "0001685", "0002123",
        "20231001", "20250531", "3", "20250610",
        "1001", "7410538612", "20191116",
        "", "", "", "", "", "", ""
    ),
    // SMR999U - Sophia Tan
    new VehicleData(
        "D", "SMR999U", "WAUZZZ8R8AA123462",
        "N", "", "1", "", "S3239822G", "Sophia Tan", "A",
        "138", "Fajar Road", "03", "51",
        "", "812904", "Hyundai  ", "Yellow", "",
        "20251231", "0001685", "0002123",
        "20231001", "20250531", "3", "20250610",
        "1001", "8649369006", "20050607",
        "", "", "", "", "", "", ""
    ),
    // SNF561A - Unregistered Vehicle
    new VehicleData(
        "D", "SNF561A", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // XD7221C - Global Trade Solutions Pte Ltd
    new VehicleData(
        "D", "XD7221C", "",
        "N", "", "", "", "", "", "",
        "", "", "", "",
        "", "", "", "", "",
        "", "", "",
        "", "", "2", "20250610",
        "1001", "", "",
        "", "", "", "", "", "", ""
    ),
    // SLF8253K - Muhammad Zaid Bin Hassan (MHA TS-NRO)
    new VehicleData(
        "D", "SLF8253K", "3FAFP31331R654326",
        "", "", "1", "", "S2948572P", "Muhammad Zaid Bin Hassan", "A",
        "25", "Jalan Tun Razak", "12", "03",
        "Menara Prisma", "50400", "Honda  ", "White", "",
        "20251231", "0001685", "0002123",
        "20231001", "", "0", "20250610",
        "1001", "1126803154", "20240104",
        "", "", "", "", "", "", ""
    ),
    // SNA927E - Liam Teo (MHA PS-RIP/PS-RP2)
    new VehicleData(
      "D", "SNA927E", "1FMZU63E94Z123462",
      "", "", "1", "", "S3343631I", "Liam Teo", "A",
      "371", "Aljunied Road", "30", "90",
      "", "609812", "Mercedes-Benz", "Brown", "",
      "20251231", "0001685", "0002123",
      "20231001", "", "0", "20250610",
      "1001", "2151195019", "20110318",
      "", "", "", "", "", "", ""
    )
  ));

  /**
   * Get vehicle data by registration number
   * @param registrationNumber Vehicle registration number
   * @return VehicleData object or null if not found
   */
  public static VehicleData getVehicleByRegistrationNumber(String registrationNumber) {
    if (registrationNumber == null) {
      return null;
    }

    for (VehicleData vehicle : VEHICLE_DATA) {
      if (registrationNumber.equals(vehicle.getVehicleRegistrationNumber())) {
        return vehicle;
      }
    }

    return null;
  }

  /**
   * Get vehicle data by chassis number
   * @param chassisNumber Vehicle chassis number
   * @return VehicleData object or null if not found
   */
  public static VehicleData getVehicleByChassisNumber(String chassisNumber) {
    if (chassisNumber == null) {
      return null;
    }

    for (VehicleData vehicle : VEHICLE_DATA) {
      if (chassisNumber.equals(vehicle.getChassisNumber())) {
        return vehicle;
      }
    }

    return null;
  }

  /**
   * Get vehicle data by owner ID
   * @param ownerId Owner ID
   * @return VehicleData object or null if not found
   */
  public static VehicleData getVehicleByOwnerId(String ownerId) {
    if (ownerId == null) {
      return null;
    }

    for (VehicleData vehicle : VEHICLE_DATA) {
      if (ownerId.equals(vehicle.getOwnerId())) {
        return vehicle;
      }
    }

    return null;
  }
}
