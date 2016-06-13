var KaplanMeierLine = React.createClass({
    render: function() {
        
    }
});

var KaplanMeierPlot = React.createClass({
    getInitialState: function() {
        return {
            width: 500,
            height: 500,
            individuals: [
                {name: "yellow", status: "Alive", tumor: "thing"},
                {name: "green", status: "Alive", tumor: "thing"},
                {name: "red", status: "Dead", tumor: "thing", days: 5},
                {name: "pink", status: "Dead", tumor: "thing", days: 8},
                {name: "black", status: "Dead", tumor: "thing", days: 13}
            ]
        }
    },

    render: function() {
        return (
            <div>
              <svg width={this.state.width} height={this.state.height}>
                <line x1="0" y1="0" x2={this.state.width} y2={this.state.height} stroke="rgb(0,100,200)" strokeWidth="5" />
              </svg>
            </div>
        );
    }
});

ReactDOM.render(<KaplanMeierPlot />, document.getElementById('kaplan-meier'));
