@param String title
@param String levelId
@param String meta
@param java.util.List<com.zincoid.gd.GdSvgText.Line> lines
@param String thumbnail
@param String noThumbnail
@param int height

<svg width="1440" height="${height * 3 / 2}" viewBox="0 0 960 ${height}" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.20"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
        <clipPath id="thumbnailClip">
            <rect x="40" y="170" width="360" height="203" rx="8"/>
        </clipPath>
    </defs>

    <rect width="960" height="${height}" fill="#0B0D12"/>
    <rect width="960" height="${height}" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="${height}" fill="#4C8DFF"/>

    <text x="40" y="50" font-family="JetBrains Mono Bold" font-size="13" letter-spacing="2.4"
          fill="#8FA0B8"># GD / LEVEL_</text>
    ${com.zincoid.gd.GdSvgText.render(title, 38, 100, 30, "#FFFFFF", null)}
    <text x="920" y="100" text-anchor="end" font-family="JetBrains Mono Bold" font-size="14"
          fill="#8FA0B8">#${levelId}</text>
    ${com.zincoid.gd.GdSvgText.render(meta, 40, 132, 14, "#C5CCDA", null)}
    <rect x="40" y="148" width="880" height="1" fill="#232A36"/>

    <rect x="40" y="170" width="360" height="203" rx="8" fill="#161A23" stroke="#232A36"/>
    @if(!thumbnail.isBlank())
        <image x="40" y="170" width="360" height="203" preserveAspectRatio="xMidYMid slice"
               clip-path="url(#thumbnailClip)" href="${thumbnail}"/>
    @else
        ${com.zincoid.gd.GdSvgText.render(noThumbnail, 220, 278, 14, "#8FA0B8", "middle")}
    @endif

    @for(com.zincoid.gd.GdSvgText.Line line : lines)
        ${com.zincoid.gd.GdSvgText.render(line, 430)}
    @endfor

    <text x="40" y="${height - 36}" font-family="JetBrains Mono Bold" font-size="11"
          letter-spacing="1.2" fill="#8FA0B8">GDDL / NULLBOT</text>
    <text x="920" y="${height - 36}" text-anchor="end" font-family="JetBrains Mono"
          font-size="12" fill="#8FA0B8">GD LEVEL</text>
</svg>
