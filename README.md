# Importer

## Installation

```bash
mvn clean install
```

## Usage

To run the web application use the following command:
```bash
java -jar target/mycore-importer-webapp.jar
```

To run the cli application use the following command:
```bash
java -jar target/mycore-importer-cli.jar
```


## Configuration

The following configuration is required in the application.properties. The application.properties should be located in the same directory as the jar file.

### Database
See Spring Documentation for more information: https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#data-properties

```properties
# H2 Database (for testing)
spring.datasource.url=jdbc:h2:file:./data/database;AUTO_SERVER=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.generate-ddl=true
```

### Import Source Configuration

#### Zenodo
The source configuration is used to configure the Zenodo instance to import the data from.

```properties
importer.zenodo-sources.source-id.url=https://zenodo.org/
importer.zenodo-sources.source-id.community=fli
```
The url is the base url of the Zenodo instance. The community is the community id of the community to import from.

#### PPN-List
The source configuration is used to configure the PPN-List to import the data from.

```properties
importer.ppn-lists.file.file-paths[0]=/Users/paschty/Downloads/ppns/0.ppn
importer.ppn-lists.file.file-paths[1]=/Users/paschty/Downloads/ppns/1.ppn
importer.ppn-lists.file.file-paths[2]=/Users/paschty/Downloads/ppns/2.ppn
```

The file-paths are the paths to the ppn files. The importer will import all ppns from the files. The files should just contain one ppn per line.
It will use the unapi interface to get the data for each ppn.

### Target Configuration
The target configuration is used to configure the mycore instance to import the data to.

```properties
importer.targets.target-id.url=http://127.0.0.1:8291/mir/
importer.targets.target-id.user=zenodo_fli_importer
importer.targets.target-id.password=itsasecret
```
The url is the base url of the mycore instance. The user and password are the credentials of the user that should be used to import the data.

### Job Configuration

A job is a configuration for a specific import. The job configuration is used to configure the import from the source to the target.

```properties
importer.jobs.source-to-target.importer=Zenodo2MyCoReImporter
importer.jobs.source-to-target.auto=true
importer.jobs.source-to-target.source-config-id=source-id
importer.jobs.source-to-target.target-config-id=target-id
importer.jobs.source-to-target.importer-config.files=modsLocation
importer.jobs.source-to-target.importer-config.base-id=mir_mods
importer.jobs.source-to-target.importer-config.genre=Zenodo resourceType zu OA genre
importer.jobs.source-to-target.importer-config.license=Zenodo Lizenz zu OA Lizenz
importer.jobs.source-to-target.importer-config.role=Zenodo type zu OA Rolle
importer.jobs.source-to-target.importer-config.status=published

```

The source-config-id and target-config-id are the ids of the source and target configurations. 
The importer is the id of the bean to use for the import. 
The importer-config is a map of key value pairs that are used to configure the bean.
The keys are the names of the configuration options of the importer. 
The values are the values for the configuration options.
The auto option is used to configure if the job should be executed automatically in a cron job or not.

The `Zenodo2MyCoReImporter` supports the following configuration options:

| Key     | Description                                                                                                                                          |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| files   | The strategy how to handle the files. Empty means dont do anything with the files. `modsLocation` means the files will be stored in the mods object. |
| base-id | The base id for the objects, that will be imported.                                                                                                  |
| genre   | The name of the mapping group which will be used to map the zenodo sourceType to a specific mycore genre                                             |
| license | The name of the mapping group which will be used to map the zenodo license to a specific mycore license                                              |
| role    | The name of the mapping group which will be used to map the zenodo type to a specific mycore role                                                    |
| status  | The status of the objects that will be imported.                                                                                                     |


The `PPNList2MyCoReImporter` supports the following configuration options:

| Key     | Description                                                                                                                                          |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| base-id | The base id for the objects, that will be imported.                                                                                                  |
| status  | The status of the objects that will be imported.                                                                                                     |



## rights
First you need to login to use the importer. The username consists of the actual username and the target id separated by a `@`. 
The password is the password of the target user. The MyCoRe user needs to have the role admin or the role importer. 
With the role you can access all areas that are associated with the target-id. This includes any jobs that are associated with the target-id, any source-id that is associated with these jobs
and all mappings which are associated with the target-id. Only the admin role can create mapping groups.

