export SQLCONNSTR_intranetocmsconndb='jdbc:sqlserver://localhost:1433;databaseName=ocmsizdb;encrypt=true;trustServerCertificate=true;user=sa;password=YourStrong!Passw0rd'

export SQLCONNSTR_eServiceocmsezconndb='jdbc:sqlserver://localhost:1433;databaseName=ocmsezdb;encrypt=true;trustServerCertificate=true;user=sa;password=YourStrong!Passw0rd'

---

To run local but connect to MGGSIT db

export SQLCONNSTR_intranetocmsconndb='jdbc:sqlserver://uraproject.database.windows.net:1433;database=ocmsintranet;user=ocmsiz_app_conn;password=P@ssw0rd!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30'

export SQLCONNSTR_eServiceocmsezconndb='jdbc:sqlserver://uraproject.database.windows.net:1433;database=ocmsinternet;user=ocmsez_app_conn;password=P@ssw0rd!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30'

export SQLCONNSTR_intranetcascomconndb='jdbc:sqlserver://uraproject.database.windows.net:1433;database=repccs;user=dbadmin;password=Mggsoftware1234!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30'

export CUSTOMCONNSTR_intranetoraocmsconndb='jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=52.187.150.218)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=XE)))?user=SYS&password=MyPassword123#&internal_logon=sysdba'

echo $SQLCONNSTR_intranetocmsconndb

echo $SQLCONNSTR_eServiceocmsizezconndb

# Check Oracle VM database connection string

echo $CUSTOMCONNSTR_intranetoraocmsconndb

---

## Oracle VM Database Connection

### Connection Details

- Host: 52.187.150.218
- Port: 1521
- Service Name: XE
- SYS Username: SYS
- SYS Password: MyPassword123#

### Connect with SQL*Plus

```
sqlplus 'SYS/MyPassword123#@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=52.187.150.218)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=XE))) AS SYSDBA'
```

### Run SQL Script with SQL*Plus

```
sqlplus 'SYS/MyPassword123#@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=52.187.150.218)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=XE))) AS SYSDBA' @create-vip-vehicle-table.sql
```

---

To build:
./gradlew clean build

To run:
./gradlew bootRun --args=--spring.profiles.active=local
