$(document).ready(function() {

	$('#searchUrlForm').submit(function() {
	    return false;
	});

	$('#url_search').click(function() {
	    var url = $('#searchUrlForm input[name=url]').val();
	    var searchDescriptor = $('#searchUrlForm select[name=searchDescriptor]').val();
	    var rerankDescriptor = $('#searchUrlForm select[name=rerankDescriptor]').val();
	    var searchCount = $('#searchUrlForm input[name=searchCount]').val();
        var rerankCount = $('#searchUrlForm input[name=rerankCount]').val();
	    search(url, searchDescriptor, rerankDescriptor, searchCount, rerankCount);
	});

});

function search(imageUrl, searchDescriptor, rerankDescriptor, searchCount, rerankCount) {
    // clear the old data
    $('.image').remove();
    $('#results').html('Please stand by .... <img src="img/loader-light.gif"/>');
    // get all the new data from the server ...
    var url = '/solr/liresolr/lireSim?url=' + imageUrl
            + '&searchDescriptor=' + searchDescriptor
            + '&rerankDescriptor=' + rerankDescriptor
            + '&searchCount=' + searchCount
            + '&rerankCount=' + rerankCount;
    $.getJSON(url, function (myResult) {
        $("#perfResults").html("Index search time: " + myResult.responseHeader.QTime + " ms");
        console.log(myResult);

        var last = $("#results");
        for (var i =0; i< myResult.docs.length; i++) {
            myID = myResult.docs[i].id.toString();
            console.log(myID);
            recent = $( '<div class="image"><img class="lireimg" src="data/' + myID +'" />'
                + 'd=' + myResult.docs[i].d + '<br/>');
            recent.insertAfter(last);
            last=recent;
        }
    });
}