<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Content Hub" hasrestapi=true> 

<div class="panel" id="webview">

<em><strong>Disclaimer</strong>: this endpoint is a proof of concept /
<strong>experimental</strong> feature. It does not actually store the content
on the disk, just in memory.</em>

<h3>Recently uploaded Content Items</h3>

<div class="storeContents">
<table>
  <tr>
    <th>Local ID</th>
    <th>Media type</th>
    <th>Enhancements <img src="${it.staticRootUrl}/images/rdf.png"
       alt="Format: RDF"/></th>
  </tr>
  <#list it.recentlyEnhancedItems as item>
  <tr>
    <td><a href="${item.uri}" title="${item.uri}">${item.localId}</a></td>
    <td>${item.mimetype}</td>
    <td><a href="${it.rootUrl}/contenthub/metadata/${item.localId}">${item.enhancements}</a></td>
  </tr>
  </#list>
</ul>
</table>
<ul class="previousNext">
  <#if it.moreRecentItemsUri?exists>
    <li class="moreRecent"><a href="${it.moreRecentItemsUri}">More recent items</a></li>
  </#if>
  <#if it.olderItemsUri?exists>
    <li class="older"><a href="${it.olderItemsUri}">Older items</a></li>
  </#if>
</ul>
</div>


<h3>Submit a new Content Item for analysis</h3>

<form method="POST" accept-charset="utf-8">
  <fieldset>
  <legend>Submit raw text content</legend>
  <p><textarea rows="15" name="content"></textarea></p>
  <p><input type="submit" value="Submit text"></p>
  </fieldset>
</form>

<form method="POST" accept-charset="utf-8">
  <fieldset>
  <legend>Submit a remote public resource by URL</legend>
  <p><input name="url" type="text" class="url" />
     <input type="submit" value="Submit URL"></p>
  </fieldset>
</form>

<form method="POST" accept-charset="utf-8"  enctype="multipart/form-data">
  <fieldset>
  <legend>Upload a local file</legend>
  <p><input name="file" type="file"/>
     <input type="submit" value="Submit file"></p>
  </fieldset>
</form>


</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Uploading new content to the Content Hub</h3>

  <p>You can upload content to the Content Hub for analysis with or without providing the content
   id at your option:<p>
  <ol>
    <li><code>PUT</code> content to <code>${it.publicBaseUri}contenthub/content/<strong>content-id</strong></code>
     with <code>Content-Type: text/plain</code>.</li>
    <li><code>GET</code> enhancements from the same URL with
     header <code>Accept: application/rdf+xml</code>.</li>
  </ol>
  
  <p><code><strong>content-id</strong></code> can be any valid path and
   will be used to fetch your item back later.</p>

  <p>On a unix-ish box you can use run the following command from
   the top-level source directory to populate the Stanbol enhancer service with
    sample content items:</p>

<pre>
for file in data/text-examples/*.txt;
do
  curl -i -X PUT -H "Content-Type:text/plain" -T $file ${it.publicBaseUri}contenthub/content/$(basename $file) ;
done
</pre> 

  Alternatively you can let the Stanbol enhancer automatically build an id base on the SHA1
  hash of the content by posting it at the root of the Content Hub.
  <ol>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/</code>
     with <code>Content-Type: text/plain</code>.</li>
    <li><code>GET</code> enhancements from the URL in the response along with
       header <code>Accept: application/rdf+xml</code>.</li>
  </ol>
  
  <p>For instance:</p>
<pre>
curl -i -X POST -H "Content-Type:text/plain" \
     --data "The Stanbol enhancer can detect famous cities such as Paris." \
     ${it.publicBaseUri}contenthub
    
HTTP/1.1 201 Created
Location: ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>


<h3>Fetching back the original content item and the related enhancements from the Content Hub</h3>

<p>Once the content is created in the Content Hub, you can fetch back either the original content, a HTML summary view or
the extracted RDF metadata by dereferencing the URL using the <code>Accept</code> header
as selection switch:<p>

<pre>
curl -i <strong>-H "Accept: text/plain"</strong> ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/<strong>raw</strong>/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<pre>
curl -i <strong>-H "Accept: text/html"</strong> ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/<strong>page</strong>/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<pre>
curl -i <strong>-H "Accept: application/rdf+xml"</strong> ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/<strong>metadata</strong>/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

</div>


</@common.page>
</#escape>
