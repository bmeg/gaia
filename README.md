# gaia

Focal point for gathering and analyzing biomedical evidence as a graph.

![GAIA](https://github.com/bmeg/gaia/blob/master/resources/gaia.jpg)

## motivation

Gaia is an event, query and analysis engine built on the Tinkerpop graph db interface. It has three primary concepts: Schemas, Facets and Agents.

### schemas

Every Gaia instance has a schema.

### facets

Every Gaia project offers a set of Facets which provide endpoints for API's and visualizations.

### agents

Every Gaia project has a set of Agents that do analysis on the graph and post results back to the graph.

## prerequisites

For heavy use cases, we use Titan/Cassandra, so if you want to use these you have to have cassandra installed and running.

Otherwise you can use the basic Tinkergraph implementation for a lightweight in memory DB.

## usage

To get Gaia up and running, clone this repo, then run

    ./bin/gaia init

This lays the initial foundation for running the db. Then you can issue a series of `ingest` commands to get data in your graph (hugo contains the gene name and gene synonym graph):

    ./bin/gaia ingest --url http://bmeg.io/data/hugo

Once you have ingested everything you need, you can start Gaia with the `start` command:

    ./bin/gaia start

Then navigate to [http://localhost:11223](http://localhost:11223)!
