export SQLCONNSTR_eServiceocmspiiezconndb='jdbc:sqlserver://uraproject.database.windows.net:1433;database=ocmspii;user=ocmspiiez_app_conn;password=P@ssw0rd!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30'
export SQLCONNSTR_eServiceocmsezconndb='jdbc:sqlserver://uraproject.database.windows.net:1433;database=ocmsinternet;user=ocmsez_app_conn;password=P@ssw0rd!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30'


export SPRING_PROFILES_ACTIVE=sit
clear && bash ./gradlew bootRun

#clear && bash ./gradlew bootRun --args=—spring.profiles.active=sit