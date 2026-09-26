#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Build a minimal, Figma-friendly system architecture SVG.

Layers (all taken from real source):
  1. Browser
  2. Spring Security (SecurityConfig + JwtAuthenticationFilter + JWT)
  3. DispatcherServlet / Controller  (Thymeleaf @Controller + REST @RestController)
  4. Service (@Service, business logic)
  5. Repository / MongoTemplate  (Spring Data MongoDB)
  6. MongoDB (database: cnj70_ecommerce)
  7. Thymeleaf / JSON Response
"""

SVG = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg"
     viewBox="0 0 1123 794"
     width="1123" height="794"
     font-family="Inter, Helvetica, Arial, sans-serif">
  <title>Sơ đồ kiến trúc tổng thể hệ thống CNJ70</title>
  <desc>Luồng kiến trúc: Browser - Spring Security - Controller - Service - Repository - MongoDB - Response</desc>

  <defs>
    <!-- Request arrow: blue, solid -->
    <marker id="arrowRequest" viewBox="0 0 12 12" refX="11" refY="6"
            markerWidth="11" markerHeight="11" orient="auto-start-reverse">
      <path d="M0,0 L12,6 L0,12 Z" fill="#1F6FEB"/>
    </marker>
    <!-- Response arrow: orange, dashed -->
    <marker id="arrowResponse" viewBox="0 0 12 12" refX="11" refY="6"
            markerWidth="11" markerHeight="11" orient="auto-start-reverse">
      <path d="M0,0 L12,6 L0,12 Z" fill="#D97706"/>
    </marker>
    <!-- Subtle shadow -->
    <filter id="cardShadow" x="-5%" y="-5%" width="110%" height="115%">
      <feDropShadow dx="0" dy="1.5" stdDeviation="2"
                    flood-color="#1F2328" flood-opacity="0.12"/>
    </filter>
  </defs>

  <!-- Page background -->
  <rect x="0" y="0" width="1123" height="794" fill="#FAFBFC"/>

  <!-- Title -->
  <text x="561.5" y="48" text-anchor="middle"
        font-size="26" font-weight="700" fill="#0F172A">
    SƠ ĐỒ KIẾN TRÚC TỔNG THỂ HỆ THỐNG
  </text>
  <text x="561.5" y="72" text-anchor="middle"
        font-size="13" font-weight="500" fill="#475569">
    Spring Boot 3.2.0 &#183; Spring Security 6 &#183; Spring Data MongoDB &#183; Thymeleaf &#183; MongoDB
  </text>

  <!-- Side rail: client -->
  <text x="60" y="120" font-size="11" font-weight="700" fill="#1F6FEB"
        letter-spacing="0.5">CLIENT TIER</text>
  <line x1="60" y1="126" x2="180" y2="126" stroke="#B6D4FE" stroke-width="1"/>

  <!-- Side rail: backend -->
  <text x="60" y="265" font-size="11" font-weight="700" fill="#6F42C1"
        letter-spacing="0.5">BACKEND TIER</text>
  <line x1="60" y1="271" x2="180" y2="271" stroke="#D2B6FE" stroke-width="1"/>

  <!-- Side rail: persistence -->
  <text x="60" y="525" font-size="11" font-weight="700" fill="#2DA44E"
        letter-spacing="0.5">PERSISTENCE TIER</text>
  <line x1="60" y1="531" x2="180" y2="531" stroke="#74C088" stroke-width="1"/>

  <!-- ============================================================ -->
  <!-- BLOCK 1: BROWSER                                              -->
  <!-- ============================================================ -->
  <g id="block-browser">
    <rect x="370" y="100" width="380" height="64" rx="10"
          fill="#FFFFFF" stroke="#1F6FEB" stroke-width="1.8"
          filter="url(#cardShadow)"/>
    <rect x="370" y="100" width="380" height="22" rx="10" fill="#1F6FEB"/>
    <rect x="370" y="112" width="380" height="10" fill="#1F6FEB"/>
    <text x="560" y="116" text-anchor="middle"
          font-size="11" font-weight="700" fill="#FFFFFF" letter-spacing="1">CLIENT</text>
    <text x="560" y="148" text-anchor="middle"
          font-size="18" font-weight="700" fill="#0F172A">Browser</text>
    <text x="560" y="158" text-anchor="middle"
          font-size="10" fill="#475569">
      HTML + CSS + JS &#183; Tailwind CSS &#183; Thymeleaf templates
    </text>
  </g>

  <!-- Arrow: Browser -> Spring Security -->
  <path d="M 560 168 L 560 196" fill="none"
        stroke="#1F6FEB" stroke-width="2" marker-end="url(#arrowRequest)"/>
  <text x="572" y="186" font-size="10" font-weight="600" fill="#1F6FEB">
    HTTP Request (JWT)
  </text>

  <!-- ============================================================ -->
  <!-- BLOCK 2: SPRING SECURITY                                      -->
  <!-- ============================================================ -->
  <g id="block-security">
    <rect x="370" y="200" width="380" height="64" rx="10"
          fill="#FFFFFF" stroke="#6F42C1" stroke-width="1.8"
          filter="url(#cardShadow)"/>
    <rect x="370" y="200" width="380" height="22" rx="10" fill="#6F42C1"/>
    <rect x="370" y="212" width="380" height="10" fill="#6F42C1"/>
    <text x="560" y="216" text-anchor="middle"
          font-size="11" font-weight="700" fill="#FFFFFF" letter-spacing="1">SECURITY</text>
    <text x="560" y="248" text-anchor="middle"
          font-size="18" font-weight="700" fill="#0F172A">Spring Security</text>
    <text x="560" y="258" text-anchor="middle"
          font-size="10" fill="#475569">
      SecurityFilterChain &#183; JwtAuthenticationFilter &#183; JWT
    </text>
  </g>

  <!-- Arrow: Security -> Controller -->
  <path d="M 560 268 L 560 296" fill="none"
        stroke="#1F6FEB" stroke-width="2" marker-end="url(#arrowRequest)"/>
  <text x="572" y="286" font-size="10" font-weight="600" fill="#1F6FEB">
    filter pass / SecurityContext
  </text>

  <!-- ============================================================ -->
  <!-- BLOCK 3: DISPATCHERSERVLET / CONTROLLER                       -->
  <!-- ============================================================ -->
  <g id="block-controller">
    <rect x="370" y="300" width="380" height="64" rx="10"
          fill="#FFFFFF" stroke="#0E7C66" stroke-width="1.8"
          filter="url(#cardShadow)"/>
    <rect x="370" y="300" width="380" height="22" rx="10" fill="#0E7C66"/>
    <rect x="370" y="312" width="380" height="10" fill="#0E7C66"/>
    <text x="560" y="316" text-anchor="middle"
          font-size="11" font-weight="700" fill="#FFFFFF" letter-spacing="1">WEB LAYER</text>
    <text x="560" y="348" text-anchor="middle"
          font-size="18" font-weight="700" fill="#0F172A">DispatcherServlet / Controller</text>
    <text x="560" y="358" text-anchor="middle"
          font-size="10" fill="#475569">
      @Controller &#183; @RestController &#183; @RequestMapping
    </text>
  </g>

  <!-- Arrow: Controller -> Service -->
  <path d="M 560 368 L 560 396" fill="none"
        stroke="#1F6FEB" stroke-width="2" marker-end="url(#arrowRequest)"/>
  <text x="572" y="386" font-size="10" font-weight="600" fill="#1F6FEB">
    gọi Service
  </text>

  <!-- ============================================================ -->
  <!-- BLOCK 4: SERVICE                                              -->
  <!-- ============================================================ -->
  <g id="block-service">
    <rect x="370" y="400" width="380" height="64" rx="10"
          fill="#FFFFFF" stroke="#BF8700" stroke-width="1.8"
          filter="url(#cardShadow)"/>
    <rect x="370" y="400" width="380" height="22" rx="10" fill="#BF8700"/>
    <rect x="370" y="412" width="380" height="10" fill="#BF8700"/>
    <text x="560" y="416" text-anchor="middle"
          font-size="11" font-weight="700" fill="#FFFFFF" letter-spacing="1">BUSINESS LOGIC</text>
    <text x="560" y="448" text-anchor="middle"
          font-size="18" font-weight="700" fill="#0F172A">Service</text>
    <text x="560" y="458" text-anchor="middle"
          font-size="10" fill="#475569">
      @Service &#183; @Transactional &#183; Lombok
    </text>
  </g>

  <!-- Arrow: Service -> Repository -->
  <path d="M 560 468 L 560 496" fill="none"
        stroke="#1F6FEB" stroke-width="2" marker-end="url(#arrowRequest)"/>
  <text x="572" y="486" font-size="10" font-weight="600" fill="#1F6FEB">
    truy vấn dữ liệu
  </text>

  <!-- ============================================================ -->
  <!-- BLOCK 5: REPOSITORY / MongoTemplate                          -->
  <!-- ============================================================ -->
  <g id="block-repository">
    <rect x="370" y="500" width="380" height="64" rx="10"
          fill="#FFFFFF" stroke="#2DA44E" stroke-width="1.8"
          filter="url(#cardShadow)"/>
    <rect x="370" y="500" width="380" height="22" rx="10" fill="#2DA44E"/>
    <rect x="370" y="512" width="380" height="10" fill="#2DA44E"/>
    <text x="560" y="516" text-anchor="middle"
          font-size="11" font-weight="700" fill="#FFFFFF" letter-spacing="1">DATA ACCESS</text>
    <text x="560" y="548" text-anchor="middle"
          font-size="18" font-weight="700" fill="#0F172A">Repository / MongoTemplate</text>
    <text x="560" y="558" text-anchor="middle"
          font-size="10" fill="#475569">
      Spring Data MongoDB &#183; MongoRepository&lt;T, String&gt;
    </text>
  </g>

  <!-- Arrow: Repository -> MongoDB -->
  <path d="M 560 568 L 560 596" fill="none"
        stroke="#1F6FEB" stroke-width="2" marker-end="url(#arrowRequest)"/>
  <text x="572" y="586" font-size="10" font-weight="600" fill="#1F6FEB">
    MongoDB Driver / BSON
  </text>

  <!-- ============================================================ -->
  <!-- BLOCK 6: MongoDB                                              -->
  <!-- ============================================================ -->
  <g id="block-mongo">
    <rect x="370" y="600" width="380" height="64" rx="10"
          fill="#FFFFFF" stroke="#0B5A8A" stroke-width="1.8"
          filter="url(#cardShadow)"/>
    <rect x="370" y="600" width="380" height="22" rx="10" fill="#0B5A8A"/>
    <rect x="370" y="612" width="380" height="10" fill="#0B5A8A"/>
    <text x="560" y="616" text-anchor="middle"
          font-size="11" font-weight="700" fill="#FFFFFF" letter-spacing="1">DATABASE</text>
    <text x="560" y="648" text-anchor="middle"
          font-size="18" font-weight="700" fill="#0F172A">MongoDB</text>
    <text x="560" y="658" text-anchor="middle"
          font-size="10" fill="#475569">
      Database: cnj70_ecommerce &#183; @Document collections
    </text>
  </g>

  <!-- ============================================================ -->
  <!-- BLOCK 7: RESPONSE (Thymeleaf / JSON)                         -->
  <!-- ============================================================ -->
  <g id="block-response">
    <rect x="800" y="100" width="240" height="64" rx="10"
          fill="#FFFFFF" stroke="#D97706" stroke-width="1.8"
          filter="url(#cardShadow)"/>
    <rect x="800" y="100" width="240" height="22" rx="10" fill="#D97706"/>
    <rect x="800" y="112" width="240" height="10" fill="#D97706"/>
    <text x="920" y="116" text-anchor="middle"
          font-size="11" font-weight="700" fill="#FFFFFF" letter-spacing="1">RESPONSE</text>
    <text x="920" y="148" text-anchor="middle"
          font-size="18" font-weight="700" fill="#0F172A">Thymeleaf / JSON</text>
    <text x="920" y="158" text-anchor="middle"
          font-size="10" fill="#475569">
      HTML view &#183; @ResponseBody
    </text>
  </g>

  <!-- ============================================================ -->
  <!-- RESPONSE PATH (dashed orange, right side)                     -->
  <!-- Goes from MongoDB -> Service -> Controller -> Security -> Browser  -->
  <!-- ============================================================ -->

  <!-- MongoDB (x=560, y=632) goes right and up to Response block at (800,132) -->
  <path d="M 750 632 L 780 632 L 780 132 L 800 132" fill="none"
        stroke="#D97706" stroke-width="2" stroke-dasharray="6 4"
        marker-end="url(#arrowResponse)"/>
  <text x="788" y="380" text-anchor="middle" font-size="10" font-weight="700"
        fill="#D97706" font-style="italic" transform="rotate(-90, 788, 380)">
    Response (HTML / JSON)
  </text>

  <!-- Note: response also flows back through Security to Browser -->
  <path d="M 800 116 L 780 116 L 780 132" fill="none"
        stroke="#D97706" stroke-width="1" stroke-dasharray="4 3"
        opacity="0"/>

  <!-- ============================================================ -->
  <!-- LEGEND                                                         -->
  <!-- ============================================================ -->
  <g id="legend" transform="translate(60, 700)">
    <rect x="0" y="0" width="1003" height="68" rx="8"
          fill="#FFFFFF" stroke="#D0D7DE"/>
    <text x="14" y="20" font-size="12" font-weight="700" fill="#0F172A">
      Chú thích luồng
    </text>

    <!-- Request arrow sample -->
    <line x1="20" y1="42" x2="60" y2="42"
          stroke="#1F6FEB" stroke-width="2" marker-end="url(#arrowRequest)"/>
    <text x="68" y="46" font-size="11" fill="#1F2328" font-weight="600">Request</text>
    <text x="68" y="60" font-size="10" fill="#57534E">
      từ Browser đi xuống qua các tầng đến MongoDB
    </text>

    <!-- Response arrow sample -->
    <line x1="330" y1="42" x2="370" y2="42"
          stroke="#D97706" stroke-width="2" stroke-dasharray="6 4"
          marker-end="url(#arrowResponse)"/>
    <text x="378" y="46" font-size="11" fill="#1F2328" font-weight="600">Response</text>
    <text x="378" y="60" font-size="10" fill="#57534E">
      từ MongoDB đi lên, qua Controller và Security về Browser
    </text>

    <!-- Layer color samples -->
    <rect x="660" y="34" width="14" height="14" rx="2" fill="#FFFFFF" stroke="#1F6FEB" stroke-width="1.5"/>
    <text x="680" y="46" font-size="11" fill="#1F2328">Client tier</text>

    <rect x="660" y="52" width="14" height="14" rx="2" fill="#FFFFFF" stroke="#6F42C1" stroke-width="1.5"/>
    <text x="680" y="63" font-size="11" fill="#1F2328">Security tier</text>

    <rect x="780" y="34" width="14" height="14" rx="2" fill="#FFFFFF" stroke="#0E7C66" stroke-width="1.5"/>
    <text x="800" y="46" font-size="11" fill="#1F2328">Web tier</text>

    <rect x="780" y="52" width="14" height="14" rx="2" fill="#FFFFFF" stroke="#BF8700" stroke-width="1.5"/>
    <text x="800" y="63" font-size="11" fill="#1F2328">Service tier</text>

    <rect x="900" y="34" width="14" height="14" rx="2" fill="#FFFFFF" stroke="#2DA44E" stroke-width="1.5"/>
    <text x="920" y="46" font-size="11" fill="#1F2328">Persistence</text>

    <rect x="900" y="52" width="14" height="14" rx="2" fill="#FFFFFF" stroke="#0B5A8A" stroke-width="1.5"/>
    <text x="920" y="63" font-size="11" fill="#1F2328">Database</text>
  </g>

  <!-- ============================================================ -->
  <!-- FOOTER                                                         -->
  <!-- ============================================================ -->
  <text x="561.5" y="784" text-anchor="middle" font-size="10" fill="#6E7781">
    Kiến trúc dựa trên source code thực tế của dự án CNJ70 (Spring Boot 3.2.0) — không liệt kê class cụ thể.
  </text>
</svg>
'''

OUT = 'd:/Code Full/Java/BTL_E_commerce/Ecommerce_Java/docs/system-architecture.svg'

# Write as UTF-8 bytes
with open(OUT, 'wb') as f:
    f.write(SVG.encode('utf-8'))

print('Wrote', len(SVG.encode('utf-8')), 'bytes to', OUT)

# Validate XML
import xml.etree.ElementTree as ET
tree = ET.parse(OUT)
root = tree.getroot()
print('Root tag :', root.tag)
print('viewBox  :', root.get('viewBox'))
print('size     :', root.get('width'), 'x', root.get('height'))
counts = {}
for el in root.iter():
    tag = el.tag.split('}')[-1]
    counts[tag] = counts.get(tag, 0) + 1
print('Elements :')
for k, v in sorted(counts.items(), key=lambda x: -x[1]):
    print('   ', k, ':', v)

# Quick content checks
text = SVG
checks = ['Browser', 'Spring Security', 'DispatcherServlet / Controller',
          'Service', 'Repository / MongoTemplate', 'MongoDB',
          'Thymeleaf / JSON', 'SecurityFilterChain', 'JwtAuthenticationFilter',
          '@Controller', '@Service', '@Transactional', 'MongoRepository',
          'cnj70_ecommerce']
for k in checks:
    print(('OK   ' if k in text else 'MISS '), k)
