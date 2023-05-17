/*
This widget uses D3.js (https://d3js.org/) JavaScript libraries to generate pretty
WordTree visualizations. It has been adapted from:
 • "Radial Tidy Tree" by Herman Sontrop:
    https://bl.ocks.org/FrissAnalytics/ffbd3cb71848616957cd4c0f41738aec
 • "d3-svg-to-png by Juan Irache
    https://github.com/JuanIrache/d3-svg-to-png
*/

document.getElementById("radslider").value = 16;
document.getElementById("fontslider").value = 12;
document.getElementById("angleslider").value = 72;

/************************************************/
/* Create SVG image                             */
/************************************************/

let data = null,
    svg = d3.select("svg"),
    g = svg.append("g"),
    depth = 0,
    angle = 360,
    radius = 160,
    zoom = d3.zoom()
        .scaleExtent([0.1, 10])
        .on("zoom", function({transform}) {
            g.attr("transform", transform);
    });

d3.select("#zoom_in").on("click", function() {
   zoom.scaleBy(svg.transition().duration(600), 1.15);
});

d3.select("#zoom_out").on("click", function() {
   zoom.scaleBy(svg.transition().duration(600), 0.85);
});

document.getElementById('words').onclick = function () {
    g.selectAll("text").text(function() {
        return d3.select(this).attr("word");
    });
};

document.getElementById('steals').onclick = function () {
    g.selectAll("text").text(function() {
        return d3.select(this).attr("steal");
    });
};

document.getElementById('playability').onclick = function () {
    zoomState = g.attr("transform");
    svg.selectAll("*").remove();
    g = svg.append("g");
    display();
    g.attr("transform", zoomState);
};


document.getElementById('editButton').onclick = function () {
    const x = document.getElementById('editorTools');
    x.style.display = x.style.display === "none" ? "table" : "none";
};

/**
 *
 */
function setLexicon(lexicon) {
    document.getElementById('lexicon').value = lexicon;
    localStorage.setItem('lexicon', lexicon);

    switch(lexicon) {
        case 'CSW21' :

            document.getElementById('copyright-notice').innerHTML = 'Collins Official Scrabble Words, an imprint of HarperCollins Publishers Limited.';
            break;

        case 'NWL20' :
            document.getElementById('copyright-notice').innerHTML = 'NASPA Word List, 2020 Edition, © 2020 North American Word Game Players Association';
            break;
    }
    return lexicon;
}

/**
 * Get lexicon (if any) and query (if any) from URL parameters;
 * otherwise load tree from localStorage
 */
window.onload = function() {

    let lexicon = (new URL(document.location)).searchParams.get("lexicon");
    if(lexicon !== null) {
        if(lexicon.match(/CSW[0-9]*/i)) {
            lexicon = setLexicon('CSW21');
        }
        else if(lexicon.match(/NWL[0-9]*/i)) {
            lexicon = setLexicon('NWL20');
        }
    }
    else
        lexicon = setLexicon(localStorage.getItem('lexicon') || 'NWL20');

    let query = (new URL(document.location)).searchParams.get("query");
    if(query !== null && query.match(/[A-Za-z]{4,15}/)) {
        search(lexicon, query.toUpperCase());
    }
    else {
        init(window.localStorage.getItem('json'));
    }

    document.getElementById('entry').addEventListener("keyup", function(event) {

        if (event.key === 'Enter') {
            event.preventDefault();
            document.querySelector('form').submit();
        }
    });

    document.getElementById('form').onsubmit = function() {
            const lexicon = document.getElementById('lexicon').value;
            const query = document.getElementById('entry').value;
            if(query !== null && query.match(/[A-Z]{4,15}/))
                search(lexicon, query.toUpperCase());
    }

}




/**
 * Fetch data from the Anagrams server
 */
function search(lexicon, query) {

    const url = 'https://anagrams.mynetgear.com/' + lexicon + '/' + query;
    fetch(url)
    .then(response => response.text())
    .then(data => {
        window.localStorage.setItem('json', data);
        window.localStorage.setItem('lexicon', lexicon);
        window.location.href = "/tree.html";
    //    window.history.pushState({}, "/tree.html/" + lexicon + "/" + query, "/tree.html/" + lexicon);
    //    location.reload();
    });
}




/**
 * Load tree data, display the tree, compute size parameters, and set zoom level
 */
function init(json) {

    if(json === null) return;

    data = JSON.parse(json);

    depth = d3.max(data, d => (d.id.match(/\./g) || [] ).length);

    display();

    let rect = g.node().getBoundingClientRect();
    svg.attr("width", Math.min(Math.max(500, rect.width), 1000));
    svg.attr("height", Math.min(Math.max(500, rect.height), 1000));
    svg.attr("viewBox", [rect.x - 50, rect.y - 50, rect.width + 100, rect.height + 100]);
    g.attr("transform", `translate(0, ${svg.node().getBoundingClientRect().y}) scale(1.0)`);
    document.getElementById("sizeslider").value = svg.attr("width")/60;

    const centered = d3.zoomIdentity.translate(0, svg.node().getBoundingClientRect().y);
    svg.call(zoom.transform, centered)
       .call(zoom);

}

/**
 *
 */
function display() {

    const stratify = d3.stratify().parentId(d => d.id.substring(0, d.id.lastIndexOf(".")));
    const tree = d3.tree()
        .size([angle, radius*depth])
        .separation((a,b) => (a.parent == b.parent ? 1 : 2) / a.depth);
    root = tree(stratify(data));
    document.title = "Word Tree: " + data[0].id;

    const link = g.selectAll(".link").data(root.descendants().slice(1)).enter()
        .append("path")
        .attr("class", "link")
        .attr("d", function(d) {
            return "M" + project(d.x, d.y)
                + "C" + project(d.x, (d.y + d.parent.y) / 2)
                + " " + project(d.parent.x, (d.y + d.parent.y) / 2)
                + " " + project(d.parent.x, d.parent.y);
            }
        )
        .attr("stroke-width", function(d) {
            if(document.getElementById('playability').checked) {
                return Math.max(Math.log2(d.data.prob), 1);
            }
            else return 1.5;
        });

    const node = g.selectAll(".node").data(root.descendants()).enter().append("g")
        .attr("class", d => "node" + (d.children ? " node--internal" : " node--leaf"))
        .attr("transform", d => `translate(${project(d.x, d.y)})`);

    node.append("circle")
        .attr("r", 2.5);

    node.append("text")
        .style("font-size", fontslider.value + "px")
        .style("font-family", "sans-serif")
        .attr("dy", ".31em")
        .attr("x", d => d.x < 180 === !d.children ? 6 : -6)
        .style("text-anchor", d => d.x < 180 === !d.children ? "start" : "end")
        .attr("transform", d => `rotate(${(d.x < 180 ? d.x - 90 : d.x + 90)})`)
        .attr("angle", d => d.children ? d.x + 180 : d.x)
        .attr("word", d => d.id.substring(d.id.lastIndexOf(".") + 1).replace(/[#$]/, ""))
        .attr("def", d => d.data.def)
        .attr("steal", d => d.depth === 0 ? d.id.substring(d.id.lastIndexOf(".") + 1) : d.data.shortsteal)
        .text(function(d) {
            if(document.getElementById("words").checked || d.depth === 0)
                return d3.select(this).attr("word");
            else
                return d3.select(this).attr("steal");
        })


        //Create the tooltip and background
        .on("mouseover", function() {
            if(this.getAttribute("def") === null) return;
            g.selectAll('.background').remove();
            g.selectAll('.tooltip').remove();
            const background = g.append('rect')
                .attr("class", "background")
                .attr('stroke', 'black')
                .attr('fill', 'yellow')
                .style("opacity", .9);
            const tip = g.append("text")
                .attr("class", "tooltip")
                .style("opacity", .9);

            //A transparent copy of the node for detecting mouse exit
            d3.select(this.parentNode)
                .clone(true).style("opacity", 1e-6).raise()
                .on("mouseout", function() {
                    g.selectAll('.background').remove();
                    g.selectAll('.tooltip').remove();
                    d3.select(this).remove();
            });

            //Layout tooltip text
           tip.html(this.getAttribute("def")).style("font-size", fontslider.value + "px");
                    let words = tip.text().split(/\s+/).reverse(),
                        word,
                        line = [],
                        numLines = 0,
                        lineHeight = fontslider.value,
                        tspan = tip.text(null).append("tspan").attr("x", 0).attr("y", 0).attr("dy", 0);
                    while (word = words.pop()) {
                        line.push(word);
                        tspan.text(line.join(" "));
                        if (tspan.node().getComputedTextLength() > 300) {
                            line.pop();
                            tspan.text(line.join(" "));
                            line = [word];
                            tspan = tip.append("tspan").attr("x", 0).attr("y", 0).attr("dy", ++numLines * lineHeight + "px").text(word);
                        }
                    }

                //Compute tooltip size and position
                const tipSize = tip.node().getBBox();
                tip
                   .attr("transform", () => `${this.parentNode.getAttribute("transform")}
                        translate(${project(this.getAttribute("angle"), this.getComputedTextLength()/2)})
                        translate(-${tipSize.width/2}, 0)`);

            background
               .attr("width", tipSize.width + 5)
               .attr("height", tipSize.height + 4)
               .attr("transform", () => `${this.parentNode.getAttribute("transform")}
                    translate(${project(this.getAttribute("angle"), this.getComputedTextLength()/2)})
                    translate(-${(tipSize.width/2+3)}, -${lineHeight})`);
        });

    /**
     * convert from radial to Cartesian coordinates
     */
    function project(ang, rad) {
        ang = (ang - 90) / 180 * Math.PI;
        return [rad * Math.cos(ang), rad * Math.sin(ang)];
    }

}

/**
 *
 */
function resizeImage() {
    const aspectRatio = svg.attr("height")/svg.attr("width");
    svg.attr("width", 60*sizeslider.value)
       .attr("height", 60*sizeslider.value*aspectRatio);
    display();
}

/**
 *
 */
function setFontSize() {
    d3.selectAll("text").style("font-size", fontslider.value + "px");
}

/**
 *
 */
function setRadius() {
    radius = 10 * radslider.value;
    zoomState = g.attr("transform");
    svg.selectAll("*").remove();
    g = svg.append("g");
    display();
    g.attr("transform", zoomState);
}

/**
 *
 */
function setAngle() {
    angle = 5 * angleslider.value;
    zoomState = g.attr("transform");
    svg.selectAll("*").remove();
    g = svg.append("g");
    display();
    g.attr("transform", zoomState);
}


/****************************************************/
/* Save image to PNG file                           */
/****************************************************/

d3.select('#saveButton').on('click', function() {
  saveImage('svg', data[0].id, {});
});

/**
 *
 */
const inlineStyles = target => {
    const selfCopyCss = elt => {
        const computed = window.getComputedStyle(elt);
        const css = {};
        for (let i = 0; i < computed.length; i++) {
            css[computed[i]] = computed.getPropertyValue(computed[i]);
        }

        for (const key in css) {
            elt.style[key] = css[key];
        }
        return css;
    };

    const root = document.querySelector(target);
    selfCopyCss(root);
    root.querySelectorAll('*').forEach(elt => selfCopyCss(elt));
};

/**
 *
 */
const copyToCanvas = ({ target, scale, format, quality }) => {
    const svg = document.querySelector(target);
    const svgData = new XMLSerializer().serializeToString(svg);
    const canvas = document.createElement('canvas');
    const svgSize = svg.getBoundingClientRect();

    canvas.width = svgSize.width * scale;
    canvas.height = svgSize.height * scale;
    canvas.style.width = svgSize.width;
    canvas.style.height = svgSize.height;

    const ctxt = canvas.getContext('2d');
    ctxt.scale(scale, scale);

    const img = document.createElement('img');
    img.setAttribute('src', 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData))));
    return new Promise(resolve => {
        img.onload = () => {
            ctxt.drawImage(img, 0, 0);
            const file = canvas.toDataURL(`image/${format}`, (format = 'png'), quality);
            resolve(file);
        };
    });

};

/**
 *
 */
const downloadImage = ({ file, name, format }) => {
    const a = document.createElement('a');
    a.download = `${name}.${format}`;
    a.href = file;
    document.body.appendChild(a);
    a.click();
    a.remove();
};

/**
 *
 */
async function saveImage(target, name, { scale = 1, format = 'png', quality = 1, download = true, ignore = null, cssinline = 1 } = {}) {
    const elt = document.querySelector(target);

    //Remove unwanted elements
    if (ignore != null) {
        const elt = document.querySelector(ignore);
        elt.parentNode.removeChild(elt)
    }

    //Set all the css styles inline
    if(cssinline === 1) {
        inlineStyles(target, ignore);
    }

    //Copy all html to a new canvas
    return await copyToCanvas({ target, scale, format, quality })
        .then(file => {
            //Download if necessary
            if (download) {
                downloadImage({ file, name, format });
            }
            return file;
        })
        .catch(console.error);
}

