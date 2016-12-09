package gaia.html

import scalatags.Text.all._
import scalatags.Text.tags2._

object VertexHtml {
  def layout(inner: Map[String, String] => Modifier): Modifier = {
    html(
      head(
        meta(name := "description", content := "Vertex Explorer"),

        link(rel := "stylesheet", href := "https://fonts.googleapis.com/css?family=Roboto:regular,bold,italic,thin,light,bolditalic,black,medium&amp;lang=en"),
        link(rel := "stylesheet", href := "https://fonts.googleapis.com/icon?family=Material+Icons"),
        link(rel := "stylesheet", href := "https://code.getmdl.io/1.1.3/material.blue_grey-light_blue.min.css"),
        link(rel := "stylesheet", href := "/static/styles.css"),

        script(src := "https://code.getmdl.io/1.1.3/material.min.js"),
        script(src := "https://ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js"),
        script(src := "https://fb.me/react-0.14.2.js"),
        script(src := "https://fb.me/react-dom-0.14.2.js"),
        script(src := "https://cdnjs.cloudflare.com/ajax/libs/babel-core/5.8.23/browser.min.js"),
        script(src := "https://cdnjs.cloudflare.com/ajax/libs/d3/3.5.6/d3.min.js"),
        script(src := "/static/vertex/js/vertex.jsx", `type` := "text/babel")
      ),

      body(`class` := "bmeg-io mdl-color--grey-100 mdl-color-text--black-700 mdl-base",
        div(`class` := "mdl-layout mdl-js-layout mdl-layout--fixed-header",
          header(`class` := "mdl-layout__header mdl-layout__header--scroll mdl-color--primary",
            div(`class` := "mdl-layout__header-row mdl-layout__tab-bar mdl-js-ripple-effect mdl-color--primary",
              img(src := "/static/bmeg-logo.png", height := "45"),
              a(href := "#graph", id := "graph-tab", `class` := "mdl-layout__tab is_active", "Evidence Graph"),
              a(href := "#overview", `class` := "mdl-layout__tab", "Overview"),
              a(href := "#data", `class` := "mdl-layout__tab", "Data Sources"),
              a(href := "#server", `class` := "mdl-layout__tab", "Software"))),

          main(`class` := "mdl-layout__content",
            div(`class` := "mdl-layout__tab-panel is-active", id := "graph",
              section(`class` := "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp",
                div(`class` := "mdl-card mdl-cell mdl-cell--12-col",
                  div(`class` := "mdl-card__supporting-text mdl-grid mdl-grid--no-spacing",
                    inner(Map("thing" -> "what"))))))))
      )
    )
  }

  def vertex(env: Map[String, String]): Modifier = {
    div(id := "vertex-explore")
  }
}
