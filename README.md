# gaia

Focal point for gathering and analyzing biomedical evidence as a graph.

![GAIA](https://github.com/bmeg/gaia/blob/master/resources/gaia.jpg)

## Usage

First you must install Titan `1.1.0-SNAPSHOT`:

```
git clone git@github.com:thinkaurelius/titan.git
cd titan
git co titan11
mvn clean install -DskipTests=true -Paurelius-release -Dgpg.skip=true
```

Then, fire up the server using sbt:

```
sbt run
```

Then navigate to [http://localhost:11223/gene/hello](http://localhost:11223/gene/hello)
