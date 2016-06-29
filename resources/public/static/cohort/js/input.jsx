var FeatureInput = React.createClass({
  getInitialState: function() {
    return {
      input: "",
      tumorCounts: {}
    };
  },

  changeInput: function(event) {
    this.setState({input: event.target.value})
    console.log(this.state.input);

    var url = "/gaea/feature/" + event.target.value + "/tumor/counts";

    $.ajax({
      url: url,
      dataType: 'json',
      type: 'GET',
      success: function(result) {
        this.setState({tumorCounts: result})
      }.bind(this),

      error: function(xhr, status, err) {
        console.error(url, status, err.toString());
      }.bind(this)
    });
  },

  render: function() {
    return (
      <div>
        <form onChange={(e) => this.changeInput(e)}>
        <input id="feature-text-input" type="text" name="feature" className="mdl-textfield__input" />
        </form>
        <p>{JSON.stringify(this.state.tumorCounts)}</p>
      </div>
    );
  }
});

ReactDOM.render(<FeatureInput />, document.getElementById('feature-input'))
