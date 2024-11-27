export function visualizeJSON(data, blockMap) {
    let html = '';
    Object.keys(data).forEach(key => {
        const value = data[key];
        const blockId = `block-${Math.random().toString(36).substring(2)}`; // Generate random block ID
        html += `<div id="${blockId}" class="block">
                    <div class="key">${key}</div>`;
        
        if (typeof key === 'string' && key === 'groovy') {
            html += `<pre class="value groovy">`;
        }
        else {
            html += `<div class="value">`;
        }

        if (typeof value === 'object') {
            html += visualizeJSON(value, blockMap);
        } else {
            html += `${value}`;
        }

        if (typeof key === 'string' && key === 'groovy') {
            html += `</pre>`;
        }
        else {
            html += `</div>`;
        }
        html += `</div>`;
        
        // Add value to the mapping object
        blockMap[blockId] = {k: key, v: value};
    });

    return html;
}