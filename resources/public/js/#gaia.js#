var Gaia = function() {
  var status = {};
  var graphs = {};

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
    return post('/status', {root: root}).then(after)
  }

  function cytoscapeFlow(flow) {
    var processNodes = Object.keys(flow.process).map(function(key) {
      var node = flow.process[key].node;
      return {
        data: {
          id: node.key,
          name: key,
          command: node.command
        }
      }
    })

    var dataNodes = Object.keys(flow.data).map(function(key) {
      var node = flow.data[key].node;
      return {
        data: {
          id: key,
          name: key,
        }
      }
    })

    var processEdges = _.flatten(Object.keys(flow.process).map(function(key) {
      var pointing = flow.process[key].to || [];
      return pointing.map(function(to) {
        return {
          data: {
            id: key,
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
            id: key,
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
    var radius = 20;
    var nodeColor = '#594346'
    var focusColor = '#7ec950'
    var activeColor = '#105a8c'
    var nodeText = '#ffffff'
    var edgeText = '#ffffff'
    var edgeColor = '#4286f4'

    var cy = cytoscape({
      container: element,
      boxSelectionEnabled: false,
      autounselectify: true,
      userZoomingEnabled: false,
      userPanningEnabled: false,

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
          'font-size': radius * 0.24, // 24
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

        .selector('edge')
        .css({
          'content': 'data(label)',
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

      elements: graph
    })

    var layout = cy.makeLayout({
      // name: 'preset',
      // positions: this.calculatePositions(width, height)
      // animate: true,
      // padding: 30,
      // animationThreshold: 250,
      // refresh: 20
    })
  }

  function load(root) {
    console.log('loading... !')
    fetchStatus(root, function(response) {
      console.log(response);
      status[root] = response['status'];
      var graph = cytoscapeFlow(status[root].flow);
      console.log(graph)
      graphs[root] = buildCytoscape('gaia', graph);
    })
  }

  return {
    post: post,
    fetchStatus: fetchStatus,
    cytoscapeFlow: cytoscapeFlow,
    load: load,
    status: status
  }
} ();
