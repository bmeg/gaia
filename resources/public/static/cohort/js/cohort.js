var cohort = [
  {'tumor': 'Pancreas', 'population': 12},
  {'tumor': 'Breast', 'population': 22},
  {'tumor': 'Prostate', 'population': 32},
];

var pie = d3.layout.pie().value(function(d) {return d.population});
var slices = pie(cohort);

var arc = d3.svg.arc().innerRadius(0).outerRadius(100);
var color = d3.scale.category10();

var svg = d3.select('svg.cohort');
var g = svg.append('g').attr('transform', 'translate(200, 200)');

g.selectAll('path.slice')
  .data(slices)
  .enter()
  .append('path')
  .attr('class', 'slice')
  .attr('d', arc)
  .attr('fill', function(d) {return color(d.data.tumor)});

