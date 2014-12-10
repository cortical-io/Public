cortical.io Retina Fingerprint Renderer
=======================================

HTML5 canvas renderer for [cortical.io](http://www.cortical.io/) Retina fingerprints to be used in conjunction with
the [cortical.io API](http://api.cortical.io/) and jQuery.

## Installation

Download and include `jquery.retinaFingerprintRenderer-1.0.js` (development version) or `jquery
.retinaFingerprintRenderer-1.0.min.js` (production version) in an HTML document after
including jQuery.

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="/path/to/jquery.retinaFingerprintRenderer-1.0.js"></script>

## Usage

The plugin can be invoked on a target DIV element after selecting it with jQuery. The element will then be transformed
into a fingerprint renderer, with the configured options applied to it. The `fingerprintSize` option has no default
value and must be set manually corresponding with the size of cortical.io Retina being used.

```javascript
var positions = ... // perform cortical.io API lookup to retrieve positions array
$("#target-div").fingerprintRenderer({
    scale: 3,
    positions: positions,
    fingerprintSize: 128
});
```

## Options

Options are passed to the plugin as an object on invocation of the `fingerprintRenderer` method. The
available options are:

<table class="table table-bordered table-striped">
	<thead>
		<tr>
			<th style="width: 100px;">Name</th>
			<th style="width: 100px;">Type</th>
			<th style="width: 50px;">Default</th>
			<th>Description</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>backgroundColor</td>
			<td>CSS color value</td>
			<td>#FFFFFF</td>
			<td>Background color of the fingerprint (if not transparent).</td>
		</tr>
		<tr>
        	<td>bitColor</td>
        	<td>CSS color value</td>
        	<td>#005570</td>
        	<td>Color used for active bits in the fingerprint.</td>
        </tr>
        <tr>
        	<td>containerBorder</td>
            <td>CSS border property value</td>
            <td>solid 2px #EDEDED</td>
            <td>CSS border property to be applied to the fingerprint container DIV.</td>
        </tr>
    	<tr>
        	<td>fingerprintSize</td>
        	<td>Number</td>
        	<td>none</td>
        	<td>Dimension of the cortical.io Retina being used.</td>
        </tr>
        <tr>
        	<td>gridColor</td>
        	<td>CSS color value</td>
            <td>#EDEDED</td>
            <td>Color of the grid overlay rendered over the fingerprint (if enabled).</td>
        </tr>
        <tr>
        	<td>gridEnabled</td>
        	<td>Boolean</td>
            <td>true</td>
            <td>Flag indicating if the grid overlay should be rendered.</td>
        </tr>
        <tr>
        	<td>mouseoverCallback</td>
        	<td>function</td>
            <td>undefined</td>
            <td>Callback function to be called as the user moves their cursor over the fingerprint. The callback
            receives a data parameter, containing the current x/y coordinates and Retina position of the
            mouse pointer. This can be used to make additional API calls to further inspect particular positions of
            the rendered fingerprint.</td>
        </tr>
        <tr>
        	<td>positions</td>
        	<td>Array</td>
            <td>[]</td>
            <td>Array of active bits in the fingerprint to render.</td>
        </tr>
        <tr>
        	<td>scale</td>
        	<td>Number</td>
            <td>1</td>
            <td>Factor to scale the size of the rendered fingerprint by. Values below 1 make the fingerprint
            smaller; values above 1 increase its size.</td>
        </tr>
        <tr>
        	<td>transparent</td>
        	<td>Boolean</td>
            <td>false</td>
            <td>Flag indicating if inactive bits of the fingerprint should be transparent. This can be used to
            layer multiple rendered fingerprints on top of each other to create complex displays of semantic
            overlap.</td>
        </tr>

	</tbody>
</table>

### Setting Default Options

When making multiple calls to the plugin it may be useful to override the standard default options in order to
simplify subsequent plugin calls. This can be done by extending the plugin's default options as follows:

```javascript
$.fn.fingerprintRenderer.defaults = $.extend({}, $.fn.fingerprintRenderer.defaults, {
	containerBorder: "",
	fingerprintSize: 128,
	gridColor: "#fafafa",
	gridEnabled: true,
    scale: 5,
    transparent: true
});
```

Once set, calls to `fingerprintRenderer` use the newly configured defaults, so even the otherwise required option
`fingerprintSize` must no longer be set manually:

```javascript
$("#target-div").fingerprintRenderer({positions: positions});
```


License
-------

Copyright 2014 cortical.io GmbH.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
