# gaia

Focal point for gathering and analyzing biomedical evidence as a graph.

![GAIA](https://github.com/bmeg/gaia/blob/master/resources/gaia.jpg)

## idea

Gaia tracks a set of keys representing files to be computed, and the network of dependent processes that compute them. At its core is some Data Store that holds these keys, which can begin life seeded with prior information. At each cycle, Gaia calculates which keys are missing and which processes are able to be run given the set of keys available, and matches these up to select a process to run whose inputs are all available and one or more outputs are missing. Once these missing keys are computed, the cycle is run again and again until either all keys are computed or no more processes can be run.

## prerequisites

In order to run Gaia, you must have access to a Funnel server and whatever Kafka cluster Funnel is using.

## config

Here is an example of Gaia configuration (living under `resources/config/gaia.clj`):

```clj
{:kafka
 {:base
  {:host "localhost"        ;; whereever your kafka cluster lives
   :port "9092"}}

 :funnel
 {:host "http://localhost:19191"
  :path ""}

 :store
 {:type :swift              ;; can also be :file
  :root ""                  ;; this will prefix all keys in the store
  :container "biostream"
  :username "swiftuser"
  :password "password"
  :url "http://10.96.11.20:5000/v2.0/tokens"
  :tenant-name "CCC"
  :tenant-id "8897b62d8a8d45f38dfd2530375fbdac"
  :region "RegionOne"}

 :flow                      ;; path to set of commands, processes, variables and agents files
 {:path "../biostream/bmeg-etl/bmeg"}}
```

Once this is all established, you can start Gaia by typing

    lein run --config resources/config/gaia.clj

in the root level of the project (or a path to whatever config file you want to use).

## commands.yaml

The format of this file is a set of keys with a description of how to run the command. This description maps onto the Task Execution Schema with some additional information about how to translate inputs and outputs into keys in the data store. Here is an example:

```yaml
    ensembl-transform:
      image_name: spanglry/ensembl-transform
      cmd: ["go", "run", "/command/run.go", "/in/gaf.gz"]
      inputs:
        GAF_GZ: /in/gaf.gz
      outputs:
        TRANSCRIPT: /out/Transcript.json
        GENE: /out/Gene.json
        EXON: /out/Exon.json
```

Under the `inputs` and `outputs` lives a map of keys to locations in the local file system where the computation took place.

## processes.yaml

These are invocations of commands defined in the `commands.yaml` file. Each one refers to a single command and provides the mapping of inputs and outputs to keys in the data store.

Here is an example of an invocation of the previous command:

```
- key: ensembl-transform
  command: ensembl-transform
  inputs:
    GAF_GZ: source/ensembl.gtf.gz
  outputs:
    TRANSCRIPT: biostream/ensembl/ensembl.Transcript.json
    GENE: biostream/ensembl/ensembl.Gene.json
    EXON: biostream/ensembl/ensembl.Exon.json
```

In the `inputs` and `outputs` maps, the keys represent those declared by the command, and the values represent what keys to store the results under in the data store. These keys can then be declared as inputs to other processes.

## vars

There is one additional concept of a `var`: values that do not come from files but are instead directly supplied by the process invocation. Here is an example.

The command is defined like so:

```
curl-extract:
  repo: https://github.com/biostream/curl-extract
  image_name: appropriate/curl
  cmd: ["curl", "{{URL}}", "-o", "/tmp/out"]
  outputs:
    OUT: /tmp/out
```

Notice the second argument to curl is embedded in curly braces. This signifies that the value will be supplied directly during the invocation. Here is how that happens:

```
- key: cancerrxgene-cellline-extract
  command: curl-extract
  vars:
    URL: ftp://ftp.sanger.ac.uk/pub/project/cancerrxgene/releases/release-6.0/Cell_Lines_Details.xlsx
  outputs:
    OUT: source/crx/cell-lines.xlsx
```

Here under the `vars` key we specify the `URL` which will be substituted into the command.

# generating all implied funnel documents

If you don't need to trigger all the funnel tasks but you would like to see what funnel tasks would be run (a dry run, so to speak), you can emit all funnel documents currently implied by the current `processes.yaml` file:

    lein run -m gaia.funnel --config path/to/config.clj --output funnel-tasks.json

The funnel tasks will be emitted in json format, one task message per line.