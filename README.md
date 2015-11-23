# kixi.hecuba.minorca
![map of minorca](http://webs.racocatala.cat/eltalp/cart2.Armstrong1752.jpg)

######Image source: http://webs.racocatala.cat/eltalp/cart2.htm

##Description:
Minorca gets CSV files from an AWS S3 bucket. Those files contains sensors measurements form houses that need to be uploaded into [getembed.com](http://www.getembed.com/).

Minorca uses AWS Java API via a Clojure library called [Amazonica](https://github.com/mcohen01/amazonica). It also uses [Hecuba API](https://github.com/MastodonC/kixi.hecuba/blob/master/doc/api.md) to add houses, devices and sensor measurements.

Minorca expects a configuration file containing the following information:
```
{:s3 {:bucket "my-bucket"
      :cred {:profile "default" :endpoint "eu-west-1"}}
 :mapping-file "resources/default-houses-mapping-file.csv"
 :processed-file "resources/default-files-processed.csv"}
```
It needs:
* information for the AWS S3 bucket,
* a file to store the relationship between the identifiers from the input files and the identifiers in getembed.com (see an example [here](https://github.com/MastodonC/kixi.hecuba.minorca/blob/master/resources/default-houses-mapping.csv)),
* a file to store which input files have been processed (see an example [here](https://github.com/MastodonC/kixi.hecuba.minorca/blob/master/resources/default-files-processed.csv)).


## Usage
Create a jar:

`$ lein uberjar`

Run your jar:
```
$ java -jar uberjar/path/kixi.hecuba.minorca-0.1.0-SNAPSHOT-standalone.jar
-i project-id -u https://api-url/1/ -n username -p password
```

## License

Copyright © 2015 MastodonC

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
