function searchIdentity(url) {
    // clear the old data
    $('.imgCont').remove();
    $('.noMatch').remove();
    $("#perfIdentity").html("Please stand by .... <img src=\"img/loader-light.gif\"/>");
    // get all the new data from the server ...
    serverUrl = "/solr/liresolr/lireId?url="+url;
    console.log(serverUrl);
    $.getJSON( serverUrl, function( myResult ) {
        $("#perfIdentity").html("Index search time: " + myResult.responseHeader.QTime + " ms");
        console.log(myResult);

        var identity = myResult.identity;

        if (identity == true) {
            var id = myResult.doc.id;
            var img = $( "<div class=\"imgCont\"><img class=\"lireimg\" src=\"data/"+id+"\" />" + "d=" + myResult.doc.d+"<br/>");
            img.insertAfter($('#identity'));
        } else {
            $("<div class=\"noMatch\">No match.</div>").insertAfter($('#identity'));
        }
    });
}

function searchSimiliar(url) {
    // clear the old data
    $('.imgCont').remove();
    $("#perfSimilar").html("Please stand by .... <img src=\"img/loader-light.gif\"/>");
    // get all the new data from the server ...
    serverUrl = "/solr/liresolr/lireSim?url="+url;
    console.log(serverUrl);
    $.getJSON( serverUrl, function( myResult ) {
        $("#perfSimilar").html("Index search time: " + myResult.responseHeader.QTime + " ms");
        console.log(myResult);

        var last = $("#similiar");
        for (var i =0; i< myResult.docs.length; i++) {
            myID = myResult.docs[i].id.toString();
            console.log(myID);
            recent = $( "<div class=\"imgCont\"><img class=\"lireimg\" src=\"data/"+myID+"\" />"
                + "d="+myResult.docs[i].d+"<br/>");
            recent.insertAfter(last);
            last=recent;
        }

    });
}
