var snipPrefix = function(s) {
  return s.substring(s.indexOf(':') + 1);
}

var exploreVertex = function(page, gid) {
  var url = "/gaea/vertex/find/" + gid;

  $.ajax({
    url: url,
    dataType: 'json',
    type: 'GET',
    success: function(result) {
      if (Object.keys(result).length > 0) {
        this.setState({vertex: result, lastMatch: page.state.input})
      }
    }.bind(page),

    error: function(xhr, status, err) {
      console.error(url, status, err.toString());
    }.bind(page)
  });
}

var VertexProperty = React.createClass({
  getInitialState: function() {
    return {};
  },

  render: function() {
    return (
      <div>
        {this.props.name} : {this.props.property}
      </div>
    );
  }
});

var VertexEdge = React.createClass({
  getInitialState: function() {
    return {};
  },

  render: function() {
    return (
      <li onClick={this.props.navigate(this.props.gid)}>{snipPrefix(this.props.gid)}</li>
    )
  }
});

var VertexEdges = React.createClass({
  getInitialState: function() {
    return {};
  },

  render: function() {
    var edges = this;
    var edgeList = this.props.edges.map(function(edge) {
      return <VertexEdge key={edge} gid={edge} navigate={edges.props.navigate} />
    })

    var prefix = this.props.edges[0].split(':')[0]

    return (
      <div>
        <h4>{this.props.label} ({this.props.direction} {prefix})</h4>
        <ul>
          {edgeList}
        </ul>
      </div>
    );
  }
});

var VertexInput = React.createClass({
  getInitialState: function() {
    return {
      input: "",
      lastMatch: "none",
      vertex: {}
    };
  },

  changeInput: function(event) {
    var gid = event.target.value
    this.setState({input: gid})
    exploreVertex(this, gid);
  },

  render: function() {
    var page = this;
    var navigate = function(gid) {
      return function() {
        page.setState({input: gid, lastMatch: gid})
        exploreVertex(page, gid);
      }
    };

    if (this.state.vertex["properties"]) {
      var properties = Object.keys(this.state.vertex.properties).map(function(key) {
        var property = page.state.vertex.properties[key];
        return <VertexProperty key={key} name={key} property={property} />
      });

      var inEdges = Object.keys(this.state.vertex['in']).map(function(key) {
        return <VertexEdges key={key} label={key} navigate={navigate} edges={page.state.vertex['in'][key]} direction="from"/>
      });

      var outEdges = Object.keys(this.state.vertex['out']).map(function(key) {
        return <VertexEdges key={key} label={key} navigate={navigate} edges={page.state.vertex['out'][key]} direction="to"/>
      });
    }

    return (
      <div>
        <form onChange={(e) => this.changeInput(e)}>
          <div className="mdl-textfield mdl-js-textfield mdl-textfield--floating-label">
            <label className="mdl-textfield__label" htmlFor="vertex-gid-input">Enter a vertex GID</label>
            <input id="vertex-gid-input" type="text" name="gid" className="mdl-textfield__input" />
          </div>
        </form>
        <h2>Showing {this.state.vertex.type} vertex {snipPrefix(this.state.lastMatch)}</h2>
        <div className="vertex-properties">
          {properties}
        </div>
        <div className="vertex-in-edges">
          <h3>In Edges</h3>
          {inEdges}
        </div>
        <div className="vertex-out-edges">
          <h3>Out Edges</h3>
          {outEdges}
        </div>
      </div>
    );
  }
});

ReactDOM.render(<VertexInput />, document.getElementById('vertex-explore'));
