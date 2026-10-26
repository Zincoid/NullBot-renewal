@param String title
@param String levelId
@param String meta
@param String stats1
@param String stats2
@param String gddl
@param String gddlExtra
@param String song
@param String description
@param String thumbnail
@param String noThumbnail
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
          fill="#8FA0B8"># GD / LEVEL_</text>
    <text x="38" y="100" font-family="Noto Sans Mono CJK SC" font-size="30"
          fill="#FFFFFF">${title}</text>
    <text x="920" y="100" text-anchor="end" font-family="JetBrains Mono Bold" font-size="14"
          fill="#8FA0B8">#${levelId}</text>
    <text x="40" y="132" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#C5CCDA">${meta}</text>
    <rect x="40" y="148" width="880" height="1" fill="#232A36"/>

    <rect x="40" y="170" width="360" height="203" rx="8" fill="#161A23" stroke="#232A36"/>
    @if(!thumbnail.isBlank())
        <image x="40" y="170" width="360" height="203" preserveAspectRatio="xMidYMid slice"
               href="${thumbnail}"/>
    @else
        <text x="220" y="278" text-anchor="middle" font-family="Noto Sans Mono CJK SC"
              font-size="14" fill="#8FA0B8">${noThumbnail}</text>
    @endif

    <text x="430" y="196" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#E7ECF5">${stats1}</text>
    <text x="430" y="228" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#E7ECF5">${stats2}</text>
    <text x="430" y="280" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#4C8DFF">${gddl}</text>
    @if(!gddlExtra.isBlank())
        <text x="430" y="306" font-family="Noto Sans Mono CJK SC" font-size="13"
              fill="#6EA8FE">${gddlExtra}</text>
    @endif
    @if(!song.isBlank())
        <text x="430" y="342" font-family="Noto Sans Mono CJK SC" font-size="13"
              fill="#C5CCDA">${song}</text>
    @endif
    @if(!description.isBlank())
        <text x="430" y="378" font-family="Noto Sans Mono CJK SC" font-size="13"
              fill="#8FA0B8">${description}</text>
    @endif

    <text x="40" y="${height - 36}" font-family="JetBrains Mono Bold" font-size="11"
          letter-spacing="1.2" fill="#8FA0B8">GDDL / NULLBOT</text>
    <text x="920" y="${height - 36}" text-anchor="end" font-family="Noto Sans Mono CJK SC"
          font-size="12" fill="#8FA0B8">GD LEVEL</text>
</svg>
