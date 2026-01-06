package com.ocmsintranet.cronservice.testing.agencies.mha.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constants for MHA file format specifications.
 * Defines the structure of input and output files for MHA NRIC upload process.
 */
public class MhaFileFormatConstants {

    // File name patterns
    public static final String INPUT_FILE_PREFIX = "URA2NRO_";
    public static final String INPUT_FILE_EXTENSION = "";
    public static final String INPUT_FILE_DATE_FORMAT = "yyyyMMddHHmmss";
    public static final String INPUT_FILE_SEARCH_PATTERN = "URA2NRO_yyyyMMdd";
    
    // Output file patterns
    public static final String OUTPUT_FILE_PREFIX = "NRO2URA_";
    public static final String OUTPUT_FILE_EXTENSION = "";
    public static final String OUTPUT_FILE_DATE_FORMAT = "yyyyMMddHHmmss";
    
    // File structure - Header Record (Input)
    public static final HeaderField[] INPUT_HEADER_FIELDS = {
        new HeaderField("Filler", 1, 9, "Text", "Spaces"),
        new HeaderField("Date of run", 10, 8, "Text", "YYYYMMDD"),
        new HeaderField("Time of Run", 18, 6, "Text", "HHMMSS"),
        new HeaderField("No of records", 24, 6, "Text", "With leading zeroes, e.g. 000012"),
        new HeaderField("Filler", 30, 4, "Text", "Spaces")
    };
    
    // File structure - Data Record (Input)
    public static final DataField[] INPUT_DATA_FIELDS = {
        new DataField("UIN", 1, 9, "Text", "S9999999A"),
        new DataField("URA Reference No.", 10, 10, "Text", ""),
        new DataField("Batch Date Time", 20, 14, "Text", "YYYYMMDDHHMISS")
    };
    
    // File structure - Header Record (Output)
    public static final HeaderField[] OUTPUT_HEADER_FIELDS = {
        new HeaderField("Filler", 1, 9, "Text", "Spaces"),
        new HeaderField("Date of run", 10, 8, "Text", "YYYYMMDD"),
        new HeaderField("Time of Run", 18, 6, "Text", "HHMMSS"),
        new HeaderField("No of records", 24, 6, "Text", "With leading zeroes, e.g. 000012"),
        new HeaderField("Filler", 30, 186, "Text", "Spaces")
    };
    
    // File structure - Data Record (Output)
    public static final DataField[] OUTPUT_DATA_FIELDS = {
        new DataField("UIN", 1, 9, "Text", "1)Valid Checkdigit, 2)Not duplicate, 3)In ascending order"),
        new DataField("Name", 10, 66, "Text", ""),
        new DataField("Date Of Birth", 76, 8, "Text", "YYYYMMDD"),
        new DataField("MHA Address", 0, 0, "Text", "Address in MHA format"),
        new DataField("MHA_Address_type", 84, 1, "Text", "MHA Code A,B,X,C,D,E,F,Q,I"),
        new DataField("MHA_Block No", 85, 10, "Text", ""),
        new DataField("MHA_Street Name", 95, 32, "Text", ""),
        new DataField("MHA_Floor No", 127, 2, "Text", ""),
        new DataField("MHA_Unit No", 129, 5, "Text", ""),
        new DataField("MHA_Building Name", 134, 30, "Text", ""),
        new DataField("Filler", 164, 4, "Text", ""),
        new DataField("MHA_New_Postal_Code", 168, 6, "Text", ""),
        new DataField("Date of Death", 174, 8, "Text", "YYYYMMDD"),
        new DataField("Life Status", 182, 1, "Text", "A - Alive, D - Dead"),
        new DataField("Invalid Address Tag", 183, 1, "Text", "MHA Code D,M,F,G,I,N,P,S"),
        new DataField("URA Reference No.", 184, 10, "Text", ""),
        new DataField("Batch Date Time", 194, 14, "Text", "YYYYMMDDHHMISS"),
        new DataField("Date of Address Change", 208, 8, "Text", "YYYYMMDD"),
        new DataField("Timestamp", 216, 23, "Text", "YYYYMMDDHHMISS with padding")
    };
    
    // Total record lengths
    public static final int INPUT_HEADER_LENGTH = 34;
    public static final int INPUT_DATA_LENGTH = 33;
    public static final int OUTPUT_HEADER_LENGTH = 215;
    public static final int OUTPUT_DATA_LENGTH = 238;
    
    /**
     * MHA test data record structure
     */
    public static class MhaTestData {
        private String uin;
        private String name;
        private String dateOfBirth;
        private String mhaAddressType;
        private String mhaBlockNo;
        private String mhaStreetName;
        private String mhaFloorNo;
        private String mhaUnitNo;
        private String mhaBuildingName;
        private String filler;
        private String mhaNewPostalCode;
        private String dateOfDeath;
        private String lifeStatus;
        private String invalidAddressTag;
        private String lastChangeAddressDate;
        private String uraReferenceNo;
        private String batchDateTime;
        
        public MhaTestData(String uin, String name, String dateOfBirth, String mhaAddressType,
                          String mhaBlockNo, String mhaStreetName, String mhaFloorNo, String mhaUnitNo,
                          String mhaBuildingName, String filler, String mhaNewPostalCode, String dateOfDeath,
                          String lifeStatus, String invalidAddressTag, String lastChangeAddressDate,
                          String uraReferenceNo, String batchDateTime) {
            this.uin = uin;
            this.name = name;
            this.dateOfBirth = dateOfBirth;
            this.mhaAddressType = mhaAddressType;
            this.mhaBlockNo = mhaBlockNo;
            this.mhaStreetName = mhaStreetName;
            this.mhaFloorNo = mhaFloorNo;
            this.mhaUnitNo = mhaUnitNo;
            this.mhaBuildingName = mhaBuildingName;
            this.filler = filler;
            this.mhaNewPostalCode = mhaNewPostalCode;
            this.dateOfDeath = dateOfDeath;
            this.lifeStatus = lifeStatus;
            this.invalidAddressTag = invalidAddressTag;
            this.lastChangeAddressDate = lastChangeAddressDate;
            this.uraReferenceNo = uraReferenceNo;
            this.batchDateTime = batchDateTime;
        }
        
        // Getters
        public String getUin() {
            return uin;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDateOfBirth() {
            return dateOfBirth;
        }
        
        public String getMhaAddressType() {
            return mhaAddressType;
        }
        
        public String getMhaBlockNo() {
            return mhaBlockNo;
        }
        
        public String getMhaStreetName() {
            return mhaStreetName;
        }
        
        public String getMhaFloorNo() {
            return mhaFloorNo;
        }
        
        public String getMhaUnitNo() {
            return mhaUnitNo;
        }
        
        public String getMhaBuildingName() {
            return mhaBuildingName;
        }
        
        public String getFiller() {
            return filler;
        }
        
        public String getMhaNewPostalCode() {
            return mhaNewPostalCode;
        }
        
        public String getDateOfDeath() {
            return dateOfDeath;
        }
        
        public String getLifeStatus() {
            return lifeStatus;
        }
        
        public String getInvalidAddressTag() {
            return invalidAddressTag;
        }
        
        public String getDateOfAddressChange() {
            return lastChangeAddressDate;
        }
        
        public String getUraReferenceNo() {
            return uraReferenceNo;
        }
        
        public String getBatchDateTime() {
            return batchDateTime;
        }
    }
    
    /**
     * Test data for MHA NRIC upload process
     */
    public static final List<MhaTestData> TEST_DATA = new ArrayList<>(Arrays.asList(
        // Beh Seng Hee
        new MhaTestData(
            "S0179709C", "Beh Seng Hee", "19800101", "A",
            "75", "Aljunied Crescent", "21", "1729",
            "", "", "479035", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Fang Wei Le
        new MhaTestData(
            "S0410379C", "Fang Wei Le", "19800101", "A",
            "418", "Bedok North Street 3", "32", "85",
            "", "", "530912", "",
            "", "", "19980410",
            "", "20250610110000"
        ),

        // Albert Bay Guo Ming
        new MhaTestData(
            "S0538313G", "Albert Bay Guo Ming", "19800101", "A",
            "64", "Boon Lay Drive", "03", "41A",
            "", "", "408973", "",
            "", "", "20240104",
            "", "20250610110000"
        ),

        // Ang Hao Ming
        new MhaTestData(
            "S0583790A", "Ang Hao Ming", "19800101", "A",
            "339", "Bukit Panjang Ring Road", "10", "1222",
            "", "", "556430", "",
            "", "", "19980410",
            "", "20250610110000"
        ),

        // Goh Yi Ming Matthew
        new MhaTestData(
            "S0773078J", "Goh Yi Ming Matthew", "19800101", "A",
            "520", "Circuit Road", "30", "60",
            "", "", "721039", "",
            "", "", "20081130",
            "", "20250610110000"
        ),

        // Baey Guo Ming
        new MhaTestData(
            "S0909878Z", "Baey Guo Ming", "19800101", "A",
            "490", "Dover Crescent", "16", "1415",
            "", "", "789403", "",
            "", "", "20240104",
            "", "20250610110000"
        ),

        // Phoon Jun De Jonathan
        new MhaTestData(
            "S1019517I", "Phoon Jun De Jonathan", "19800101", "A",
            "323", "Eunos Crescent", "19", "15A",
            "", "", "389042", "",
            "", "", "20191116",
            "", "20250610110000"
        ),

        // Tang Kai Ling Mark
        new MhaTestData(
            "S1124953A", "Tang Kai Ling Mark", "19800101", "A",
            "191", "Fernvale Road", "15", "1117",
            "", "", "657280", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Chong Yong Chang
        new MhaTestData(
            "S1245937H", "Chong Yong Chang", "19800101", "A",
            "177", "Hillview Avenue", "04", "63",
            "", "", "437891", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // Zahra Neha
        new MhaTestData(
            "S1323798J", "Zahra Neha", "19800101", "A",
            "523", "Jurong West Street 42", "05", "1543",
            "", "", "610239", "",
            "", "", "20191116",
            "", "20250610110000"
        ),

        // Joey Li
        new MhaTestData(
            "S1365734C", "Joey Li", "19800101", "A",
            "364", "Lengkok Bahru", "09", "76",
            "", "", "821034", "",
            "", "", "20090704",
            "", "20250610110000"
        ),

        // Siti Binte Mohamed
        new MhaTestData(
            "S1837452K", "Siti Binte Mohamed", "19800101", "A",
            "509", "Marine Terrace", "39", "7B",
            "", "", "550089", "",
            "", "", "20160623",
            "", "20250610110000"
        ),

        // Lindy Pan Rui En
        new MhaTestData(
            "S1924189J", "Lindy Pan Rui En", "19800101", "A",
            "439", "Redhill Close", "08", "34",
            "", "", "466293", "",
            "", "", "20040820",
            "", "20250610110000"
        ),

        // Grace Wong
        new MhaTestData(
            "S1928372A", "Grace Wong", "19800101", "A",
            "413", "Sengkang East Way", "12", "1578",
            "", "", "310487", "",
            "", "", "20001123",
            "", "20250610110000"
        ),

        // Danielle Pereira
        new MhaTestData(
            "S1928374R", "Danielle Pereira", "19800101", "A",
            "397", "Serangoon North Avenue 1", "37", "1462",
            "", "", "307985", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Zara Menon
        new MhaTestData(
            "S1928375J", "Zara Menon", "19800101", "A",
            "536", "Teban Gardens Road", "29", "71A",
            "", "", "458301", "",
            "", "", "20040820",
            "", "20250610110000"
        ),

        // Anita Nair
        new MhaTestData(
            "S1928376S", "Anita Nair", "19800101", "A",
            "352", "Woodlands Avenue 6", "40", "64A",
            "", "", "600124", "",
            "", "", "20001123",
            "", "20250610110000"
        ),

        // Nurul Amani Binte Zainal
        new MhaTestData(
            "S1928379J", "Nurul Amani Binte Zainal", "19800101", "A",
            "479", "Yung Kuang Road", "17", "1891",
            "", "", "569302", "",
            "", "", "20051023",
            "", "20250610110000"
        ),

        // Sim Pek Meng
        new MhaTestData(
            "S1938475G", "Sim Pek Meng", "19800101", "A",
            "584", "Bishan Street 14", "38", "1693",
            "", "", "482013", "20260609",
            "D", "", "20030513",
            "", "20250610110000"
        ),

        // Mohd Ridwan
        new MhaTestData(
            "S2162413F", "Mohd Ridwan", "19800101", "A",
            "287", "Anchorvale Road", "23", "58",
            "", "", "547902", "",
            "", "", "20001123",
            "", "20250610110000"
        ),

        // Cheong Cheng Boon
        new MhaTestData(
            "S2299218Z", "Cheong Cheng Boon", "19800101", "A",
            "222", "Bendeemer Road", "28", "1759",
            "", "", "758902", "",
            "", "", "20090704",
            "", "20250610110000"
        ),

        // Luke Tan Yu Wei
        new MhaTestData(
            "S2305924Z", "Luke Tan Yu Wei", "19800101", "A",
            "60", "Boon Keng Road", "22", "53",
            "", "", "640210", "",
            "", "", "19950803",
            "", "20250610110000"
        ),

        // Huang Yong Rui
        new MhaTestData(
            "S2514421Z", "Huang Yong Rui", "19800101", "A",
            "6", "Bukit Ho Swee Crescent", "31", "38",
            "", "", "521398", "",
            "", "", "19971007",
            "", "20250610110000"
        ),

        // Nathaniel Lee
        new MhaTestData(
            "S2792886B", "Nathaniel Lee", "19800101", "A",
            "480", "Edgefield Plains", "33", "1024",
            "", "", "611032", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Anjali Rajesh
        new MhaTestData(
            "S2837465N", "Anjali Rajesh", "19800101", "A",
            "44", "Haig Road", "36", "2045",
            "", "", "678012", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Hafiz Bin Abdullah
        new MhaTestData(
            "S2837468B", "Hafiz Bin Abdullah", "19800101", "A",
            "341", "Holland Drive", "14", "97",
            "", "", "418905", "",
            "", "D", "20000923",
            "", "20250610110000"
        ),

        // Rohit Singh - TS-NRO invalidAddressTag
        new MhaTestData(
            "S2847561X", "Rohit Singh", "19800101", "A",
            "22", "Jurong East Avenue 1", "06", "79",
            "", "", "562310", "",
            "", "M", "20000923",
            "", "20250610110000"
        ),

        // Rohit Singh - TS-NRO, street = NA && postal code 000000
        new MhaTestData(
            "S2170362A", "Rohit Singh Tsnro", "19800101", "A",
            "", "NA", "", "",
            "", "", "000000", "",
            "", "", "20000923",
            "", "20250610110000"
        ),

        // Goh Siang Hwee
        new MhaTestData(
            "S2847563H", "Goh Siang Hwee", "19800101", "A",
            "241", "Kim Tian Road", "25", "81",
            "", "", "449812", "",
            "", "F", "20051023",
            "", "20250610110000"
        ),

        // Natalie Gomez
        new MhaTestData(
            "S2847567F", "Natalie Gomez", "19800101", "A",
            "232", "Lorong Ah Soo", "13", "93A",
            "", "", "557209", "",
            "", "G", "19980410",
            "", "20250610110000"
        ),

        // Isabella Nair
        new MhaTestData(
            "S2847569F", "Isabella Nair", "19800101", "A",
            "400", "MacPherson Lane", "27", "25",
            "", "", "693215", "",
            "", "I", "20001123",
            "", "20250610110000"
        ),

        // Andie Fung Jun Jie
        new MhaTestData(
            "S2943554E", "Andie Fung Jun Jie", "19800101", "A",
            "504", "Pipit Road", "18", "73",
            "", "", "705981", "",
            "", "N", "20001123",
            "", "20250610110000"
        ),

        // Hannah Lim
        new MhaTestData(
            "S2948571K", "Hannah Lim", "19800101", "A",
            "453", "Rivervale Crescent", "35", "1929",
            "", "", "573012", "",
            "", "P", "19950803",
            "", "20250610110000"
        ),

        // Muhammad Zaid Bin Hassan
        new MhaTestData(
            "S2948572P", "Muhammad Zaid Bin Hassan", "19800101", "A",
            "25", "Jalan Tun Razak", "12", "03",
            "Menara Prisma", "", "50400", "",
            "", "S", "20240104",
            "", "20250610110000"
        ),

        // Yong Jun Wei
        new MhaTestData(
            "S2948573S", "Yong Jun Wei", "19800101", "A",
            "93", "Tampines Avenue 5", "20", "1332",
            "", "", "309781", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // Tan Hui Yee Jolene
        new MhaTestData(
            "S2948574K", "Tan Hui Yee Jolene", "19800101", "A",
            "55", "Telok Blangah Crescent", "34", "1239",
            "", "", "455603", "",
            "", "", "20160623",
            "", "20250610110000"
        ),

        // Chua Wei Heng
        new MhaTestData(
            "S2948576W", "Chua Wei Heng", "19800101", "A",
            "207", "Whampoa Drive", "11", "42",
            "", "", "429810", "",
            "", "", "19980410",
            "", "20250610110000"
        ),

        // Jonathan Raj
        new MhaTestData(
            "S2948577B", "Jonathan Raj", "19800101", "A",
            "238", "Woodlands Circle", "21", "47",
            "", "", "734920", "",
            "", "", "19980410",
            "", "20250610110000"
        ),

        // Arun Pillai
        new MhaTestData(
            "S2948579T", "Arun Pillai", "19800101", "A",
            "270", "Clementi West Street 2", "32", "1932",
            "", "", "569120", "",
            "", "", "19980410",
            "", "20250610110000"
        ),

        // Sophia Tan - PS-RIP
        new MhaTestData(
            "S3239822G", "Sophia Tan", "19800101", "A",
            "138", "Fajar Road", "03", "51",
            "", "", "812904", "20260609",
            "D", "", "20050607",
            "", "20250610110000"
        ),

        // Liam Teo
        new MhaTestData(
            "S3343631I", "Liam Teo", "19800101", "A",
            "371", "Aljunied Road", "30", "90",
            "", "", "609812", "20260609",
            "D", "", "20110318",
            "", "20250610110000"
        ),

        // Julian Pereira
        new MhaTestData(
            "S3540476G", "Julian Pereira", "19800101", "A",
            "333", "Bedok South Avenue 3", "01", "36",
            "", "", "407231", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Phoon Kang Min Angeline
        new MhaTestData(
            "S3664936D", "Phoon Kang Min Angeline", "19800101", "A",
            "294", "Boon Tiong Road", "16", "48",
            "", "", "400238", "",
            "", "", "19950803",
            "", "20250610110000"
        ),

        // Zara Binte Malik
        new MhaTestData(
            "S3847561G", "Zara Binte Malik", "19800101", "A",
            "393", "Bukit Purmei Road", "19", "68",
            "", "", "345809", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Hafiz Bin Ramli
        new MhaTestData(
            "S3847562T", "Hafiz Bin Ramli", "19800101", "A",
            "603", "Commonwealth Crescent", "15", "88",
            "", "", "541093", "",
            "", "", "19971007",
            "", "20250610110000"
        ),

        // Jasmine Chua
        new MhaTestData(
            "S3847563Y", "Jasmine Chua", "19800101", "A",
            "377", "Dakota Crescent", "04", "1930",
            "", "", "499802", "",
            "", "", "20030513",
            "", "20250610110000"
        ),

        // Daniel Pereira
        new MhaTestData(
            "S3847564L", "Daniel Pereira", "19800101", "A",
            "596", "Dover Road", "02", "65",
            "", "", "602983", "",
            "", "", "20170901",
            "", "20250610110000"
        ),

        // Emily Fernandez
        new MhaTestData(
            "S3847565C", "Emily Fernandez", "19800101", "A",
            "424", "Hougang Street 11", "05", "49",
            "", "", "419082", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Maya Singh
        new MhaTestData(
            "S3847566G", "Maya Singh", "19800101", "A",
            "147", "Jalan Kayu", "09", "55",
            "", "", "619204", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Farhan Amir Bin Iskandar
        new MhaTestData(
            "S3847567Q", "Farhan Amir Bin Iskandar", "19800101", "A",
            "42", "Jurong West Avenue 1", "39", "2C",
            "", "", "546329", "",
            "", "", "20090805",
            "", "20250610110000"
        ),

        // Claudia Rodrigues
        new MhaTestData(
            "S3847568U", "Claudia Rodrigues", "19800101", "A",
            "519", "Lorong Lew Lian", "08", "78B",
            "", "", "580239", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Jiahao Tan
        new MhaTestData(
            "S3847569X", "Jiahao Tan", "19800101", "A",
            "438", "New Upper Changi Road", "12", "22",
            "", "", "688923", "",
            "", "", "20030509",
            "", "20250610110000"
        ),

        // Arjun Singh
        new MhaTestData(
            "S3948571C", "Arjun Singh", "19800101", "A",
            "368", "Pasir Ris Street 51", "37", "46",
            "", "", "527340", "",
            "", "", "20040820",
            "", "20250610110000"
        ),

        // Claudia Fernandez
        new MhaTestData(
            "S3948572P", "Claudia Fernandez", "19800101", "A",
            "353", "Sembawang Drive", "29", "1551",
            "", "", "347211", "",
            "", "", "20110318",
            "", "20250610110000"
        ),

        // Danish Amirul Bin Hassan
        new MhaTestData(
            "S3948576L", "Danish Amirul Bin Hassan", "19800101", "A",
            "572", "Serangoon Central", "40", "40A",
            "", "", "466281", "",
            "", "", "20170901",
            "", "20250610110000"
        ),

        // Yeh Xin En
        new MhaTestData(
            "S3986804J", "Yeh Xin En", "19800101", "A",
            "434", "Sin Ming Road", "17", "39",
            "", "", "309724", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Goh Beng Kiat
        new MhaTestData(
            "S4092745Z", "Goh Beng Kiat", "19800101", "A",
            "476", "Tampines Street 11", "38", "62",
            "", "", "543217", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Karl Chong Zheng En
        new MhaTestData(
            "S4121729D", "Karl Chong Zheng En", "19800101", "A",
            "25", "Toa Payoh Central", "23", "1783",
            "", "", "554098", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Jayne Goh Zhi En
        new MhaTestData(
            "S4352762B", "Jayne Goh Zhi En", "19800101", "A",
            "96", "Woodlands Drive 70", "28", "50",
            "", "", "729384", "",
            "", "", "20001123",
            "", "20250610110000"
        ),

        // Gunjan Raj d/o P. Nair
        new MhaTestData(
            "S4633840E", "Gunjan Raj d/o P. Nair", "19800101", "A",
            "445", "Yishun Ring Road", "22", "14",
            "", "", "455287", "",
            "", "", "20040820",
            "", "20250610110000"
        ),

        // Farhan Hakim Bin Ramli
        new MhaTestData(
            "S4758392J", "Farhan Hakim Bin Ramli", "19800101", "B",
            "173", "Anderson Road", "31", "98A",
            "", "", "640734", "",
            "", "", "19910621",
            "", "20250610110000"
        ),

        // Mei Ling Goh
        new MhaTestData(
            "S4758394Y", "Mei Ling Goh", "19800101", "B",
            "279", "Ardmore Park", "07", "43",
            "Amber Crest Residences", "", "615089", "",
            "", "", "20090704",
            "", "20250610110000"
        ),

        // Sanjay Pillai
        new MhaTestData(
            "S4859373D", "Sanjay Pillai", "19800101", "B",
            "58", "Balmoral Road", "33", "1474",
            "Bayshore Luxe", "", "610483", "",
            "", "", "20240104",
            "", "20250610110000"
        ),

        // Marcus D’Silva
        new MhaTestData(
            "S4859376Q", "Marcus D’Silva", "19800101", "B",
            "7", "Bukit Timah Road", "24", "1101",
            "The Verdancia", "", "439820", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Lucas Fernandez
        new MhaTestData(
            "S4867374J", "Lucas Fernandez", "19800101", "B",
            "318", "Cairnhill Road", "36", "37",
            "Skyhaven @ Newton", "", "421093", "",
            "", "", "20081130",
            "", "20250610110000"
        ),

        // Lew Xin Ling Hayley
        new MhaTestData(
            "S4870313E", "Lew Xin Ling Hayley", "19800101", "B",
            "435", "Cavenagh Road", "14", "56",
            "Oceanique Suites", "", "534082", "",
            "", "", "20111029",
            "", "20250610110000"
        ),

        // Rajesh Kumar
        new MhaTestData(
            "S4958371R", "Rajesh Kumar", "19800101", "B",
            "283", "Clemenceau Avenue", "06", "11C",
            "Parc Rivière", "", "708912", "",
            "", "", "20160623",
            "", "20250610110000"
        ),

        // Wei Xuan Lim - PS-RP2
        new MhaTestData(
            "S4958372H", "Wei Xuan Lim", "19800101", "B",
            "510", "Devonshire Road", "25", "3A",
            "The Gladescape", "", "567390", "20250527",
            "D", "", "20240104",
            "", "20250610110000"
        ),

        // Calvin Tan
        new MhaTestData(
            "S4958374Z", "Calvin Tan", "19800101", "B",
            "101", "East Coast Road", "13", "9A",
            "Riviera Heights", "", "389203", "20250527",
            "D", "", "20240104",
            "", "20250610110000"
        ),

        // Marcus Lim
        new MhaTestData(
            "S4958375V", "Marcus Lim", "19800101", "B",
            "120", "Farrer Road", "27", "75B",
            "Solaris Cove", "", "503812", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // Ariyathi Rajan
        new MhaTestData(
            "S4958376U", "Ariyathi Rajan", "19800101", "B",
            "457", "Grange Road", "18", "4A",
            "One Belvedere", "", "650901", "",
            "", "", "20110318",
            "", "20250610110000"
        ),

        // Caleb Chan
        new MhaTestData(
            "S4958377H", "Caleb Chan", "19800101", "B",
            "594", "Holland Road", "35", "6B",
            "The Amberlyn", "", "540298", "",
            "", "", "20000923",
            "", "20250610110000"
        ),

        // Sanjana Ramesh
        new MhaTestData(
            "S4958378M", "Sanjana Ramesh", "19800101", "B",
            "482", "Keppel Bay Drive", "26", "1344",
            "Eastpoint Residences", "", "619431", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // Benjamin Goh
        new MhaTestData(
            "S4958379D", "Benjamin Goh", "19800101", "B",
            "92", "Leonie Hill", "20", "5B",
            "Hillcrest Vue", "", "348920", "",
            "", "", "19950803",
            "", "20250610110000"
        ),

        // Mirah Mastura
        new MhaTestData(
            "S5072796C", "Mirah Mastura", "19800101", "B",
            "121", "Meyer Road", "34", "74",
            "Marina Belle", "", "538240", "",
            "", "", "20051023",
            "", "20250610110000"
        ),

        // Yussuf Ridwan
        new MhaTestData(
            "S5243142E", "Yussuf Ridwan", "19800101", "B",
            "615", "Mount Sinai Drive", "11", "1215",
            "Urban Dales", "", "629380", "",
            "", "", "19910621",
            "", "20250610110000"
        ),

        // John Fong Jia Sheng
        new MhaTestData(
            "S5266727E", "John Fong Jia Sheng", "19800101", "B",
            "517", "Newton Road", "21", "84",
            "Westwood Solace", "", "614523", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Syed Bin Alman Adi
        new MhaTestData(
            "S5200287G", "Syed Bin Alman Adi", "19800101", "B",
            "375", "River Valley Road", "32", "52",
            "Seaview Edge", "", "559274", "",
            "", "", "19971007",
            "", "20250610110000"
        ),

        // Deng Kok Soon
        new MhaTestData(
            "S5456829J", "Deng Kok Soon", "19800101", "B",
            "428", "Tanglin Road", "03", "86",
            "The Echelon Grove", "", "483920", "",
            "", "", "20191116",
            "", "20250610110000"
        ),

        // Chong Ming De Martin
        new MhaTestData(
            "S5511571J", "Chong Ming De Martin", "19800101", "B",
            "298", "Tomlinson Road", "10", "26",
            "Arcadia Rise", "", "737291", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Xu Kai Wen
        new MhaTestData(
            "S5592806A", "Xu Kai Wen", "19800101", "B",
            "616", "Anderson Road", "30", "70",
            "Oasis Nova", "", "573920", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Ayden Lum
        new MhaTestData(
            "S5716874I", "Ayden Lum", "19800101", "B",
            "310", "Ardmore Park", "01", "99C",
            "Verdant Peaks", "", "467905", "",
            "", "", "20051023",
            "", "20250610110000"
        ),

        // Faridah Binte Ismail
        new MhaTestData(
            "S5738294Z", "Faridah Binte Ismail", "19800101", "B",
            "583", "Balmoral Road", "16", "1923",
            "Serene Vibe Residences", "", "398201", "",
            "", "", "20050607",
            "", "20250610110000"
        ),

        // Meera Sundaram
        new MhaTestData(
            "S5829374M", "Meera Sundaram", "19800101", "B",
            "296", "Cavenagh Road", "04", "94",
            "Celestique Towers", "", "449021", "",
            "", "", "20001123",
            "", "20250610110000"
        ),

        // Lin Jun Jie
        new MhaTestData(
            "S5920493I", "Lin Jun Jie", "19800101", "B",
            "606", "Clemenceau Avenue", "02", "28",
            "Montclair Suites", "", "509132", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Nur Sharifah
        new MhaTestData(
            "S5946772G", "Nur Sharifah", "19800101", "B",
            "259", "Devonshire Road", "05", "45",
            "The Grandure", "", "555789", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Marcus Lee
        new MhaTestData(
            "S5968471E", "Marcus Lee", "19800101", "B",
            "99", "East Coast Road", "09", "21C",
            "Crestvale Residence", "", "466913", "",
            "", "", "20090805",
            "", "20250610110000"
        ),

        // Isabelle Fernandez
        new MhaTestData(
            "S5968472W", "Isabelle Fernandez", "19800101", "B",
            "53", "Farrer Road", "39", "19",
            "Nova Verde", "", "530048", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Nur Aisyah Binte Rahman
        new MhaTestData(
            "S5968473V", "Nur Aisyah Binte Rahman", "19800101", "B",
            "61", "Grange Road", "08", "20",
            "Capri ", "", "732912", "",
            "", "", "20050607",
            "", "20250610110000"
        ),

        // Olivia Pereira
        new MhaTestData(
            "S5968474E", "Olivia Pereira", "19800101", "B",
            "38", "Holland Road", "12", "8C",
            "Edenstone @ Holland", "", "642903", "",
            "", "", "20030513",
            "", "20250610110000"
        ),

        // Xue Ying Goh
        new MhaTestData(
            "S5968475N", "Xue Ying Goh", "19800101", "B",
            "87", "Keppel Bay Drive", "37", "1480",
            "Skyline Vantage", "", "579684", "",
            "", "", "20030513",
            "", "20250610110000"
        ),

        // Sanjay Nadarajan
        new MhaTestData(
            "S6005041D", "Sanjay Nadarajan", "19800101", "B",
            "71", "Leonie Hill", "29", "89",
            "The Luminaire", "", "553890", "",
            "", "", "20090805",
            "", "20250610110000"
        ),

        // Siti Mariam Binte Abdullah
        new MhaTestData(
            "S6197217Z", "Siti Mariam Binte Abdullah", "19800101", "B",
            "9", "Meyer Road", "40", "1838",
            "Harbourlight Residences", "", "541208", "",
            "", "", "20051023",
            "", "20250610110000"
        ),

        // Nathan Koh
        new MhaTestData(
            "S6207221J", "Nathan Koh", "19800101", "B",
            "49", "Mount Sinai Drive", "17", "1654",
            "Alpine Grove", "", "319204", "",
            "", "", "19910621",
            "", "20250610110000"
        ),

        // Xie Mei Fen
        new MhaTestData(
            "S6314367G", "Xie Mei Fen", "19800101", "B",
            "82", "Newton Road", "38", "1729",
            "The Vervena", "", "577089", "20250527",
            "D", "", "20240104",
            "", "20250610110000"
        ),

        // Clara Fernandez
        new MhaTestData(
            "S6499012H", "Clara Fernandez", "19800101", "B",
            "89", "River Valley Road", "23", "85",
            "Coastal Blu", "", "439812", "",
            "", "", "20051023",
            "", "20250610110000"
        ),

        // Chloe Lee
        new MhaTestData(
            "S6005044I", "Chloe Lee", "19800101", "B",
            "103", "Tanglin Road", "28", "03",
            "Elysian Court", "", "610589", "",
            "", "", "20000923",
            "", "20250610110000"
        ),

        // Andy Lau
        new MhaTestData(
            "S6005048A", "Andy Lau", "19800101", "B",
            "4", "Tomlinson Road", "22", "1222",
            "Palmvista Enclave", "", "462087", "",
            "", "", "20050607",
            "", "20250610110000"
        ),

        // Xinyi Chew
        new MhaTestData(
            "S6194325J", "Xinyi Chew", "19800101", "B",
            "108", "Bukit Timah Road", "31", "60",
            "The Montview", "", "349782", "",
            "", "", "20090704",
            "", "20250610110000"
        ),

        // Tan Yong Quan
        new MhaTestData(
            "S6485767C", "Tan Yong Quan", "19800101", "B",
            "80", "Cairnhill Road", "33", "1415",
            "Marigold Mansions", "", "538190", "",
            "", "", "20090704",
            "", "20250610110000"
        ),

        // Yusoff Radin
        new MhaTestData(
            "S6739520D", "Yusoff Radin", "19800101", "B",
            "52", "Bukit Timah Road", "25", "1543",
            "Tranquil Cove", "", "470239", "",
            "", "", "20110318",
            "", "20250610110000"
        ),

        // Aminah Binte Rahman
        new MhaTestData(
            "S6928374A", "Aminah Binte Rahman", "19800101", "C",
            "39", "Adelphi Park Avenue", "50", "64A",
            "", "", "537029", "",
            "", "", "20081130",
            "", "20250610110000"
        ),

        // Sofia Fernandez
        new MhaTestData(
            "S7017343C", "Sofia Fernandez", "19800101", "C",
            "69", "Binjai Park", "50", "1891",
            "", "", "329341", "",
            "", "", "19950803",
            "", "20250610110000"
        ),

        // Lu Sing Yu Leon
        new MhaTestData(
            "S7042651Z", "Lu Sing Yu Leon", "19800101", "C",
            "84", "Braddell Heights Estate", "51", "1693",
            "", "", "478293", "",
            "", "", "20170901",
            "", "20250610110000"
        ),

        // Annie Choong Yan Ling
        new MhaTestData(
            "S7142295Z", "Annie Choong Yan Ling", "19800101", "C",
            "24", "Bukit Teresa Road", "51", "58",
            "", "", "455002", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Ajesh Mohan s/o A. Mohan
        new MhaTestData(
            "S7320030Z", "Ajesh Mohan s/o A. Mohan", "19800101", "C",
            "95", "Carlisle Road", "52", "1759",
            "", "", "556093", "",
            "", "", "20000923",
            "", "20250610110000"
        ),

        // Beckham Li Zheng De
        new MhaTestData(
            "S7341287J", "Beckham Li Zheng De", "19800101", "C",
            "97", "Casuarina Road", "52", "53",
            "", "", "346720", "",
            "", "", "20170901",
            "", "20250610110000"
        ),

        // Chan Wai Ming
        new MhaTestData(
            "S7362894E", "Chan Wai Ming", "19800101", "C",
            "63", "Chun Tin Road", "52", "38",
            "", "", "419287", "",
            "", "", "20030509",
            "", "20250610110000"
        ),

        // Navind Kumar s/o N. Kirbakaran
        new MhaTestData(
            "S7373080E", "Navind Kumar s/o N. Kirbakaran", "19800101", "C",
            "62", "Coronation Road West", "53", "1247",
            "", "", "508390", "",
            "", "", "20191116",
            "", "20250610110000"
        ),

        // Davin Kraal
        new MhaTestData(
            "S7395379J", "Davin Kraal", "19800101", "C",
            "78", "Duchess Avenue", "53", "1024",
            "", "", "599082", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Wong Siao Yee
        new MhaTestData(
            "S7409134B", "Wong Siao Yee", "19800101", "C",
            "5", "Eastwood Road", "53", "92",
            "", "", "618209", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // Lionel Linwood Tupper
        new MhaTestData(
            "S7450757C", "Lionel Linwood Tupper", "19800101", "C",
            "26", "Frankel Avenue", "54", "2045",
            "", "", "730482", "",
            "", "", "20090805",
            "", "20250610110000"
        ),

        // Zeng Jia Jia
        new MhaTestData(
            "S7473816H", "Zeng Jia Jia", "19800101", "C",
            "54", "Greenwood Avenue", "54", "97",
            "", "", "680913", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Elijah Tan
        new MhaTestData(
            "S7506556F", "Elijah Tan", "19800101", "C",
            "88", "Jalan Angin Laut", "55", "79",
            "", "", "524091", "",
            "", "", "20170901",
            "", "20250610110000"
        ),

        // Shalini Sandra d/o J. Navin
        new MhaTestData(
            "S7654005E", "Shalini Sandra d/o J. Navin", "19800101", "C",
            "102", "Jalan Chempaka Kuning", "55", "81",
            "", "", "448291", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Grace Raj
        new MhaTestData(
            "S7762736G", "Grace Raj", "19800101", "C",
            "67", "Kheam Hock Road", "55", "93A",
            "", "", "577903", "",
            "", "", "20160623",
            "", "20250610110000"
        ),

        // Karthik Subramaniam
        new MhaTestData(
            "S7893314C", "Karthik Subramaniam", "19800101", "C",
            "28", "Loyang Rise", "56", "25",
            "", "", "555004", "",
            "", "", "19910621",
            "", "20250610110000"
        ),

        // Nur Khadijah Binte Sharul Nizam s/o Iqbal
        new MhaTestData(
            "S8067405H", "Nur Khadijah Binte Sharul Nizam s/o Iqbal", "19800101", "C",
            "48", "Namly Avenue", "56", "73",
            "", "", "460034", "",
            "", "", "20030513",
            "", "20250610110000"
        ),

        // Emmanuel Prakash s/o P. Chandrasekaran
        new MhaTestData(
            "S8081820C", "Emmanuel Prakash s/o P. Chandrasekaran", "19800101", "C",
            "85", "Opera Estate", "56", "1929",
            "", "", "627984", "",
            "", "", "20110318",
            "", "20250610110000"
        ),

        // Samuel Rajan
        new MhaTestData(
            "S8137692A", "Samuel Rajan", "19800101", "C",
            "50", "Pasir Panjang Hill", "57", "23A",
            "", "", "539087", "",
            "", "", "20090805",
            "", "20250610110000"
        ),

        // Asif Rajan
        new MhaTestData(
            "S8234567D", "Asif Rajan", "19800101", "C",
            "11", "Sunset Way", "57", "1332",
            "", "", "459210", "",
            "", "", "20110318",
            "", "20250610110000"
        ),

        // Lee Shu Kwan Elicia
        new MhaTestData(
            "S8243452F", "Lee Shu Kwan Elicia", "19800101", "C",
            "20", "Belmont Road", "58", "1239",
            "", "", "395218", "",
            "", "", "19980410",
            "", "20250610110000"
        ),

        // Zhuo Xin Ling Faith
        new MhaTestData(
            "S8635945F", "Zhuo Xin Ling Faith", "19800101", "C",
            "8", "Bukit Sedap Road", "58", "47",
            "", "", "508412", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // David Liang Jie Tan
        new MhaTestData(
            "S8643812G", "David Liang Jie Tan", "19800101", "C",
            "29", "Cactus Road", "59", "1932",
            "", "", "738429", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Vanessa Gomez
        new MhaTestData(
            "S8815638B", "Vanessa Gomez", "19800101", "C",
            "1", "Clover Avenue", "59", "51",
            "", "", "570348", "",
            "", "", "20051023",
            "", "20250610110000"
        ),

        // Tan Kah Chin Ben
        new MhaTestData(
            "S8886499I", "Tan Kah Chin Ben", "19800101", "C",
            "94", "Daffodil Drive", "59", "1064",
            "", "", "417280", "",
            "", "", "20170901",
            "", "20250610110000"
        ),

        // Lukas Schneider
        new MhaTestData(
            "S8889024H", "Lukas Schneider", "19800101", "C",
            "17", "Eden Park", "60", "90",
            "", "", "438129", "",
            "", "", "20001123",
            "", "20250610110000"
        ),

        // Du Yi Min
        new MhaTestData(
            "S9012384Z", "Du Yi Min", "19800101", "C",
            "47", "Figaro Street", "60", "36",
            "", "", "503027", "",
            "", "", "19950803",
            "", "20250610110000"
        ),

        // Anita Menon
        new MhaTestData(
            "S9107346C", "Anita Menon", "19800101", "C",
            "13", "Hua Guan Avenue", "61", "68",
            "", "", "410982", "",
            "", "", "20111029",
            "", "20250610110000"
        ),

        // Hakimah Binte Adha
        new MhaTestData(
            "S9228609F", "Hakimah Binte Adha", "19800101", "C",
            "41", "Jalan Haji Alias", "62", "1930",
            "", "", "619240", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Ella Wong
        new MhaTestData(
            "S9245177A", "Ella Wong", "19800101", "C",
            "81", "Jalan Lim Tai See", "62", "65",
            "", "", "543018", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Yeo Soon Kiat
        new MhaTestData(
            "S9283746F", "Yeo Soon Kiat", "19800101", "C",
            "30", "Kembangan Estate", "62", "49",
            "", "", "409320", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Suresh Kumar
        new MhaTestData(
            "S9458701H", "Suresh Kumar", "19800101", "C",
            "74", "Nassim Road", "63", "2C",
            "", "", "608971", "",
            "", "", "20160623",
            "", "20250610110000"
        ),

        // Sylvia Gan
        new MhaTestData(
            "S9563105C", "Sylvia Gan", "19800101", "C",
            "12", "Paya Lebar Crescent", "63", "78B",
            "", "", "530923", "",
            "", "", "19910621",
            "", "20250610110000"
        ),

        // Govind Kumar s/o P. Ravi
        new MhaTestData(
            "S9568387H", "Govind Kumar s/o P. Ravi", "19800101", "C",
            "73", "Seletar Hills Drive", "64", "22",
            "", "", "424593", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // Asha Devi
        new MhaTestData(
            "S9640091H", "Asha Devi", "19800101", "C",
            "56", "Siglap Road", "64", "46",
            "", "", "531219", "",
            "", "", "20050607",
            "", "20250610110000"
        ),

        // Ethan Lim
        new MhaTestData(
            "S9812346F", "Ethan Lim", "19800101", "C",
            "66", "Brighton Crescent", "66", "62",
            "", "", "652384", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Faridah Binte Osman
        new MhaTestData(
            "S9912360E", "Faridah Binte Osman", "19800101", "C",
            "14", "Garlick Avenue", "66", "1783",
            "", "", "710234", "",
            "", "", "20091012",
            "", "20250610110000"
        ),

        // Nathan Lim Yi Jie
        new MhaTestData(
            "S9966967E", "Nathan Lim Yi Jie", "19800101", "C",
            "10", "Jalan Binchang", "66", "50",
            "", "", "359219", "",
            "", "", "20131129",
            "", "20250610110000"
        ),

        // Pun Jia Qi
        new MhaTestData(
            "T0429178B", "Pun Jia Qi", "19800101", "C",
            "605", "Trevose Crescent", "69", "37",
            "", "", "509218", "",
            "", "", "20030509",
            "", "20250610110000"
        ),

        // Amelia Wong
        new MhaTestData(
            "T0654816J", "Amelia Wong", "19800101", "C",
            "505", "Wilkie Road", "69", "11C",
            "", "", "607398", "",
            "", "", "19980410",
            "", "20250610110000"
        ),

        // Hope Oon Hui Qi
        new MhaTestData(
            "T0849361D", "Hope Oon Hui Qi", "19800101", "A",
            "414", "Bishan Street 13", "76", "1545",
            "", "", "689120", "",
            "", "", "20110318",
            "", "20250610110000"
        ),

        // Sanjay Kumar
        new MhaTestData(
            "T0982274C", "Sanjay Kumar", "19800101", "A",
            "130", "Bedok Reservoir Road", "79", "1480",
            "", "", "408039", "",
            "", "", "20090704",
            "", "20250610110000"
        ),

        // Choong Xin En
        new MhaTestData(
            "T1640196F", "Choong Xin En", "19800101", "A",
            "262", "Ang Mo Kio Avenue 3", "81", "41A",
            "", "", "420150", "",
            "", "", "20111029",
            "", "20250610110000"
        ),

        // Hong Hao Xin
        new MhaTestData(
            "T1968820D", "Hong Hao Xin", "19800101", "A",
            "288", "Bukit Batok West Avenue 5", "29", "42",
            "", "", "648291", "",
            "", "", "20160718",
            "", "20250610110000"
        ),

        // Khairul Azmi Bin Iman Radin
        new MhaTestData(
            "T3336002E", "Khairul Azmi Bin Iman Radin", "19800101", "A",
            "152", "Bukit Merah View", "29", "47",
            "", "", "570839", "",
            "", "", "20110318",
            "", "20250610110000"
        ),

        // Mary Ng Yi Min
        new MhaTestData(
            "T3854707G", "Mary Ng Yi Min", "19800101", "A",
            "289", "Chai Chee Road", "29", "1932",
            "", "", "466782", "",
            "", "", "19971007",
            "", "20250610110000"
        ),

        // Jasmine Shreshtha d/o N. Kangatharan
        new MhaTestData(
            "T3966007A", "Jasmine Shreshtha d/o N. Kangatharan", "19800101", "A",
            "160", "Choa Chu Kang Avenue 3", "29", "51",
            "", "", "543910", "",
            "", "", "20040820",
            "", "20250610110000"
        ),

        // Li Jia Sheng Anthony
        new MhaTestData(
            "T4191728D", "Li Jia Sheng Anthony", "19800101", "A",
            "125", "Clementi Avenue 2", "30", "1064",
            "", "", "418032", "",
            "", "", "19950803",
            "", "20250610110000"
        ),

        // Aravind Rajan
        new MhaTestData(
            "T5226834B", "Aravind Rajan", "19800101", "A",
            "195", "Commonwealth Drive", "30", "90",
            "", "", "409731", "",
            "", "", "20111029",
            "", "20250610110000"
        ),

        // Neha Kumar
        new MhaTestData(
            "T5340176C", "Neha Kumar", "19800101", "A",
            "231", "Geylang Bahru", "30", "36",
            "", "", "609184", "",
            "", "", "20030513",
            "", "20250610110000"
        ),

        // Elaine Chia Xin Ling
        new MhaTestData(
            "T6493521B", "Elaine Chia Xin Ling", "19800101", "A",
            "205", "Hougang Avenue 8", "30", "48",
            "", "", "434890", "",
            "", "", "20090704",
            "", "20250610110000"
        ),

        // Zhi Wei Lim
        new MhaTestData(
            "T7719079H", "Zhi Wei Lim", "19800101", "A",
            "183", "Jalan Bukit Merah", "30", "68",
            "", "", "521903", "",
            "", "", "20080225",
            "", "20250610110000"
        ),

        // Lim Seng Hee
        new MhaTestData(
            "T8057272C", "Lim Seng Hee", "19800101", "A",
            "112", "Jurong East Street 21", "08", "88",
            "", "", "567928", "",
            "", "", "19950803",
            "", "20250610110000"
        ),

        // Allen Law Guo Ming
        new MhaTestData(
            "T9024066D", "Allen Law Guo Ming", "19800101", "A",
            "141", "Marsiling Drive", "16", "1930",
            "", "", "639502", "",
            "", "", "20040820",
            "", "20250610110000"
        ),

        // Sharul Nizam Bin Abdul Halim
        new MhaTestData(
            "T9203214G", "Sharul Nizam Bin Abdul Halim", "19800101", "A",
            "168", "Pasir Ris Drive 6", "11", "65",
            "", "", "525810", "",
            "", "", "20150915",
            "", "20250610110000"
        ),

        // Aloysius Tan Wei Jie
        new MhaTestData(
            "T9391106C", "Aloysius Tan Wei Jie", "19800101", "A",
            "116", "Punggol Field", "07", "49",
            "", "", "460120", "",
            "", "", "20081130",
            "", "20250610110000"
        ),

        // Sim Jun Wei
        new MhaTestData(
            "T9466749B", "Sim Jun Wei", "19800101", "A",
            "214", "Sembawang Crescent", "02", "05",
            "", "", "419762", "",
            "", "", "20191116",
            "", "20250610110000"
        ),

        // Alfan Yusof
        new MhaTestData(
            "T9661609G", "Alfan Yusof", "19800101", "A",
            "139", "Tampines Street 83", "07", "2C",
            "", "", "545091", "",
            "", "", "19910621",
            "", "20250610110000"
        ),

        // Erica Sim Jia Wen
        new MhaTestData(
            "T9716729F", "Erica Sim Jia Wen", "19800101", "A",
            "163", "Toa Payoh Lorong 6", "26", "78B",
            "", "", "587230", "",
            "", "", "20030509",
            "", "20250610110000"
        ),

        // Kavitha Raj d/o P. Kumar
        new MhaTestData(
            "T9832149C", "Kavitha Raj d/o P. Kumar", "19800101", "A",
            "158", "Yishun Avenue 5", "26", "22",
            "", "", "652981", "",
            "", "", "20191116",
            "", "20250610110000"
        )


    ));
    
    /**
     * Get MHA test data by UIN
     * @param uin UIN to search for
     * @return MhaTestData object or null if not found
     */
    public static MhaTestData getTestDataByUin(String uin) {
        if (uin == null) {
            return null;
        }
        
        for (MhaTestData data : TEST_DATA) {
            if (uin.equals(data.getUin())) {
                return data;
            }
        }
        
        return null;
    }
    
    /**
     * Represents a field in the header record.
     */
    public static class HeaderField {
        private final String name;
        private final int startPosition;
        private final int size;
        private final String type;
        private final String remarks;
        
        public HeaderField(String name, int startPosition, int size, String type, String remarks) {
            this.name = name;
            this.startPosition = startPosition;
            this.size = size;
            this.type = type;
            this.remarks = remarks;
        }
        
        public String getName() {
            return name;
        }
        
        public int getStartPosition() {
            return startPosition;
        }
        
        public int getSize() {
            return size;
        }
        
        public String getType() {
            return type;
        }
        
        public String getRemarks() {
            return remarks;
        }
    }
    
    /**
     * Represents a field in the data record.
     */
    public static class DataField {
        private final String name;
        private final int startPosition;
        private final int size;
        private final String type;
        private final String remarks;
        
        public DataField(String name, int startPosition, int size, String type, String remarks) {
            this.name = name;
            this.startPosition = startPosition;
            this.size = size;
            this.type = type;
            this.remarks = remarks;
        }
        
        public String getName() {
            return name;
        }
        
        public int getStartPosition() {
            return startPosition;
        }
        
        public int getSize() {
            return size;
        }
        
        public String getType() {
            return type;
        }
        
        public String getRemarks() {
            return remarks;
        }
    }
}
