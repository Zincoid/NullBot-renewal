@param String title
@param String headLevel
@param String headDifficulty
@param String headAuthor
@param String headStars
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

    <text x="40" y="50" font-family="JetBrains Maple Mono" font-weight="bold" font-size="13"
          letter-spacing="2.4" fill="#8FA0B8"># GD / SEARCH_</text>
    <text x="38" y="100" font-family="JetBrains Maple Mono" font-size="28"
          fill="#FFFFFF">${title}</text>
    <rect x="40" y="124" width="880" height="1" fill="#232A36"/>

    <text x="40" y="158" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#8FA0B8">#</text>
    <text x="76" y="158" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#8FA0B8">${headLevel}</text>
    <text x="337" y="158" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#8FA0B8">${headDifficulty}</text>
    <text x="472" y="158" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#8FA0B8">${headAuthor}</text>
    <text x="625" y="158" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#8FA0B8">${headStars}</text>

    @for(int i = 0; i < rows.size(); i++)
        <text x="40" y="${192 + i * 34}" font-family="JetBrains Maple Mono" font-size="15"
              fill="#E7ECF5">${rows.get(i)}</text>
    @endfor

    <text x="40" y="${height - 40}" font-family="JetBrains Maple Mono" font-size="13"
          fill="#8FA0B8">${prompt}</text>
    <text x="920" y="${height - 40}" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="11" letter-spacing="1.2" fill="#4C8DFF">GDDL</text>
</svg>
