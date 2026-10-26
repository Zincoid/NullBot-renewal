@param String title
@param java.util.List<com.zincoid.nullbot.module.render.svg.layout.SvgCell> cells
@param String prompt
@param int height

<svg width="1440" height="${height * 3 / 2}" viewBox="0 0 960 ${height}" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.035"/>
            <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="960" height="${height}" fill="#0E0F11"/>
    <rect width="960" height="${height}" fill="url(#glow)"/>

    <text x="40" y="50" font-family="JetBrains Maple Mono" font-weight="bold" font-size="14"
          letter-spacing="2.4" fill="#C9CFD8"># GD / SEARCH_</text>
    <text x="38" y="100" font-family="JetBrains Maple Mono" font-size="30"
          fill="#F2F3F5">${title}</text>
    <rect x="40" y="124" width="880" height="1" fill="#2A2D33"/>

    @for(com.zincoid.nullbot.module.render.svg.layout.SvgCell cell : cells)
        <text x="${cell.x()}" y="${cell.y()}" text-anchor="${cell.anchor()}"
              font-family="JetBrains Maple Mono" font-size="${cell.size()}"
              fill="${cell.color()}">${cell.text()}</text>
    @endfor

    <text x="40" y="${height - 40}" font-family="JetBrains Maple Mono" font-size="14"
          fill="#C9CFD8">${prompt}</text>
    <text x="920" y="${height - 40}" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" letter-spacing="1.2" fill="#C9CFD8">GDDL</text>
</svg>
