@param String title
@param java.util.List<String> rows
@param String prompt
@param int height

<svg width="1440" height="${height * 3 / 2}" viewBox="0 0 960 ${height}" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.20"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="960" height="${height}" fill="#0B0D12"/>
    <rect width="960" height="${height}" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="${height}" fill="#4C8DFF"/>

    <text x="40" y="50" font-family="JetBrains Mono Bold" font-size="13" letter-spacing="2.4"
          fill="#8FA0B8"># GD / SEARCH_</text>
    ${com.zincoid.gd.GdSvgText.render(title, 38, 100, 28, "#FFFFFF", null)}
    <rect x="40" y="124" width="880" height="1" fill="#232A36"/>

    @for(int i = 0; i < rows.size(); i++)
        ${com.zincoid.gd.GdSvgText.render(rows.get(i), 40, 158 + i * 34, 15, "#E7ECF5", null)}
    @endfor

    ${com.zincoid.gd.GdSvgText.render(prompt, 40, height - 40, 13, "#8FA0B8", null)}
    <text x="920" y="${height - 40}" text-anchor="end" font-family="JetBrains Mono Bold"
          font-size="11" letter-spacing="1.2" fill="#4C8DFF">GDDL</text>
</svg>
