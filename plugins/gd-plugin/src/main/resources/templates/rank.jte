@param String user
@param String labelB20
@param String score
@param String stats1
@param String stats2
@param java.util.List<String> rows
@param String note
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
          fill="#8FA0B8"># GD / B20_</text>
    ${com.zincoid.gd.GdSvgText.render(user, 38, 100, 24, "#FFFFFF", null)}
    ${com.zincoid.gd.GdSvgText.render(labelB20, 40, 142, 13, "#8FA0B8", null)}
    <text x="38" y="198" font-family="JetBrains Mono Bold" font-size="54"
          fill="#4C8DFF">${score}</text>
    ${com.zincoid.gd.GdSvgText.render(stats1, 40, 232, 14, "#E7ECF5", null)}
    ${com.zincoid.gd.GdSvgText.render(stats2, 40, 258, 14, "#C5CCDA", null)}
    <rect x="40" y="282" width="880" height="1" fill="#232A36"/>

    @for(int i = 0; i < rows.size(); i++)
        ${com.zincoid.gd.GdSvgText.render(rows.get(i), 40, 318 + i * 32, 14, "#E7ECF5", null)}
    @endfor

    ${com.zincoid.gd.GdSvgText.render(note, 40, height - 36, 12, "#8FA0B8", null)}
    <text x="920" y="${height - 36}" text-anchor="end" font-family="JetBrains Mono Bold"
          font-size="11" letter-spacing="1.2" fill="#4C8DFF">GDDL</text>
</svg>
