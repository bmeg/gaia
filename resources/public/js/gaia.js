var Gaia = function() {
  var state = {
    status: {},
    graphs: {},
    cytoscape: {}
  }

  function post(url, body) {
    return fetch(url, {
      method: 'post',
      headers: {
        'Accept': 'application/json, text/plain, */*',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    }).then(function(response) {return response.json()})
  }

  function fetchStatus(root, after) {
    return post('status', {root: root}).then(after)
  }

  function edge(source, target) {
    return "(" + source + ")-->(" + target + ")";
  }

  function cytoscapeFlow(flow) {
    var processNodes = Object.keys(flow.process).map(function(key) {
      var node = flow.process[key].node;
      return {
        data: {
          id: node.key,
          name: key,
          command: node.command,
          process: true,
          type: 'process'
        }
      }
    })

    var dataNodes = Object.keys(flow.data).map(function(key) {
      var node = flow.data[key].node;
      return {
        data: {
          id: key,
          name: key,
          data: true,
          type: 'data'
        }
      }
    })

    var processEdges = _.flatten(Object.keys(flow.process).map(function(key) {
      var pointing = flow.process[key].to || [];
      return pointing.map(function(to) {
        return {
          data: {
            id: edge(key, to),
            name: key,
            source: key,
            target: to,
            label: key
          }
        }
      })
    }))

    var dataEdges = _.flatten(Object.keys(flow.data).map(function(key) {
      var pointing = flow.data[key].to || [];
      return pointing.map(function(to) {
        return {
          data: {
            id: edge(key, to),
            name: key,
            source: key,
            target: to,
            label: key
          }
        }
      })
    }))

    return {
      nodes: processNodes.concat(dataNodes),
      edges: processEdges.concat(dataEdges)
    }
  }

  function buildCytoscape(id, graph) {
    var element = document.getElementById(id);
    var radius = 200;
    var nodeColor = '#594346'
    var focusColor = '#7ec950'
    var activeColor = '#105a8c'
    var processColor = '#105a8c'
    var dataColor = '#7eb950'
    var nodeText = '#ffffff'
    var edgeText = '#ffffff'
    var edgeColor = '#4286f4'

    console.log(element);

    var layout = {
      name: 'breadthfirst',

      fit: true, // whether to fit the viewport to the graph
      directed: true, // whether the tree is directed downwards (or edges can point in any direction if false)
      padding: 30, // padding on fit
      circle: false, // put depths in concentric circles if true, put depths top down if false
      spacingFactor: 1.75, // positive spacing factor, larger => more space between nodes (N.B. n/a if causes overlap)
      boundingBox: undefined, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
      avoidOverlap: true, // prevents node overlap, may overflow boundingBox if not enough space
      nodeDimensionsIncludeLabels: false, // Excludes the label when calculating node bounding boxes for the layout algorithm
      roots: undefined, // the roots of the trees
      maximalAdjustments: 0, // how many times to try to position the nodes in a maximal way (i.e. no backtracking)
      animate: false, // whether to transition the node positions
      animationDuration: 500, // duration of animation in ms if enabled
      animationEasing: undefined, // easing of animation if enabled,
      animateFilter: function ( node, i ){ return true; }, // a function that determines whether the node should be animated.  All nodes animated by default on animate enabled.  Non-animated nodes are positioned immediately when the layout starts
      ready: undefined, // callback on layoutready
      stop: undefined, // callback on layoutstop
      transform: function (node, position ){ return position; } // transform a given node position. Useful for changing flow direction in discrete layouts
    }

    var elements = graph.nodes.concat(graph.edges);
    console.log(elements)

    var cy = cytoscape({
      container: element,
      // boxSelectionEnabled: false,
      // autounselectify: true,
      // userZoomingEnabled: false,
      // userPanningEnabled: false,

      style: cytoscape.stylesheet()
        .selector('node')
        // .selector('node[!active]')
        .css({
          'content': 'data(name)',
          'height': radius, // 80
          'width': radius, // 80
          'background-fit': 'cover',
          'background-color': nodeColor,
          // 'border-color': '#000',
          // 'border-width': 3,
          // 'border-opacity': 0.5,
          // 'shape': 'roundrectangle',
          'color': nodeText,
          'font-family': '"Lucida Sans Unicode", "Lucida Grande", sans-serif',
          'font-size': radius * 0.20, // 24
          'text-outline-color': nodeColor,
          'text-outline-width': radius * 0.03, // 3,
          'text-valign': 'center'
        })

        .selector('node[?active]')
        .css({
          'background-color': activeColor,
        })

        .selector('node[?focus]')
        .css({
          'background-color': focusColor,
        })

        .selector('node[?process]')
        .css({
          'background-color': processColor,
          'shape': 'rectangle'
        })

        .selector('node[?data]')
        .css({
          'background-color': dataColor,
        })

        .selector('edge')
        .css({
          // 'content': 'data(label)',
          'width': radius * 0.06,
          'edge-text-rotation': 'autorotate',
          'target-arrow-shape': 'triangle',
          'line-color': edgeColor,
          'target-arrow-color': edgeColor,
          'curve-style': 'bezier',
          'color': edgeText,
          'font-size': radius * 0.18, // 18
          'text-outline-color': edgeColor,
          'text-outline-width': radius * 0.03, // 2
        }),

      layout: layout,
      elements: elements
    })

    return cy;

  }

  function vivagraph(flow) {
    var graph = Viva.Graph.graph();

    _.each(_.keys(flow.process), function(key) {
      var node = flow.process[key].node;
      var to = flow.process[key].to || []
      graph.addNode(key, {
        command: node.command,
        process: true
      });

      _.each(to, function(target) {
        graph.addLink(key, target);
      })
    })

    _.each(_.keys(flow.data), function(key) {
      var node = flow.data[key].node;
      var to = flow.data[key].to || []
      graph.addNode(key, {
        data: true
      });

      _.each(to, function(target) {
        graph.addLink(key, target);
      })
    })

    var renderer = Viva.Graph.View.renderer(graph);
    renderer.run();

    return {
      graph: graph,
      renderer: renderer
    }
  }

  function load(root) {
    console.log('loading... !')
    fetchStatus(root, function(response) {
      console.log(response);
      var status = response['status'];
      var graph = vivagraph(status.flow);

      // var graph = cytoscapeFlow(status.flow);
      // state.cytoscape = buildCytoscape('gaia', graph);
      // console.log(graph)

      state.status[root] = status;
      state.graphs[root] = graph;
    });
  }

  state.load = load;
  return state;
} ();
