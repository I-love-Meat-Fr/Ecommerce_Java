"""
Build report DOCX for BTL_CNJava_N05 covering sections 2.1, 2.2, 2.6, 3.1, 3.2, 3.3.
Strict adherence to source code in:
  d:/Code Full/Java/BTL_E_commerce/Ecommerce_Java
"""

import os
import sys
import io
from docx import Document
from docx.shared import Pt, Cm, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.enum.table import WD_ALIGN_VERTICAL, WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement


# ---------------------------------------------------------------------------
# Low-level helpers
# ---------------------------------------------------------------------------

def set_run_font(run, *, bold=False, italic=False, size=13, name="Times New Roman", color=None):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    if color is not None:
        run.font.color.rgb = color
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = OxmlElement('w:rFonts')
        rPr.append(rFonts)
    rFonts.set(qn('w:ascii'), name)
    rFonts.set(qn('w:hAnsi'), name)
    rFonts.set(qn('w:cs'), name)
    rFonts.set(qn('w:eastAsia'), name)


def add_paragraph(container, text, *, bold=False, italic=False, size=13,
                  align=WD_ALIGN_PARAGRAPH.JUSTIFY, indent_first=True,
                  space_before=0, space_after=6, line_spacing=1.5,
                  keep_with_next=False, color=None, style=None):
    if style is not None:
        p = container.add_paragraph(text, style=style)
    else:
        p = container.add_paragraph()
        run = p.add_run(text)
        set_run_font(run, bold=bold, italic=italic, size=size, color=color)
    p.alignment = align
    pf = p.paragraph_format
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    pf.line_spacing = line_spacing
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    if indent_first:
        pf.first_line_indent = Cm(1.27)
    if keep_with_next:
        pf.keep_with_next = True
    return p


def add_heading(container, text, level=1, *, align=WD_ALIGN_PARAGRAPH.LEFT):
    if level == 1:
        size = 16
        align_eff = WD_ALIGN_PARAGRAPH.CENTER
        space_before = 18
        space_after = 12
    elif level == 2:
        size = 14
        align_eff = align
        space_before = 12
        space_after = 8
    else:
        size = 13
        align_eff = align
        space_before = 8
        space_after = 6

    p = container.add_paragraph()
    run = p.add_run(text)
    set_run_font(run, bold=True, size=size)
    p.alignment = align_eff
    pf = p.paragraph_format
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    pf.line_spacing = 1.5
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    if level > 1:
        pf.first_line_indent = Cm(1.27)
    pf.keep_with_next = True
    return p


def add_bullet(container, text, *, level=0, indent_first=False):
    """Render a bullet using '-' or '+' followed by the text."""
    bullet = "- " if level == 0 else "+ "
    p = container.add_paragraph()
    run = p.add_run(bullet + text)
    set_run_font(run, size=13)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(4)
    pf.line_spacing = 1.5
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    pf.left_indent = Cm(1.27 + level * 0.6)
    if indent_first:
        pf.first_line_indent = Cm(0)
    else:
        pf.first_line_indent = Cm(0)
    return p


def add_letter_item(container, letter, text):
    """Render an a), b), c)... item with a hanging label."""
    p = container.add_paragraph()
    run = p.add_run(f"{letter}) {text}")
    set_run_font(run, bold=False, size=13)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    pf = p.paragraph_format
    pf.space_before = Pt(2)
    pf.space_after = Pt(4)
    pf.line_spacing = 1.5
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    pf.left_indent = Cm(1.27)
    pf.first_line_indent = Cm(-0.8)
    return p


def add_code_block(container, code_text):
    """Render a monospace code block with light gray background."""
    p = container.add_paragraph()
    run = p.add_run(code_text)
    rPr = run._element.get_or_add_rPr()
    rFonts = OxmlElement('w:rFonts')
    rFonts.set(qn('w:ascii'), 'Consolas')
    rFonts.set(qn('w:hAnsi'), 'Consolas')
    rFonts.set(qn('w:cs'), 'Consolas')
    rPr.append(rFonts)
    run.font.size = Pt(11)
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    pf = p.paragraph_format
    pf.space_before = Pt(4)
    pf.space_after = Pt(8)
    pf.line_spacing = 1.15
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    pf.left_indent = Cm(1.0)
    pf.right_indent = Cm(1.0)
    # Add a single-cell table to give a soft background
    return p


def add_code_in_table(container, code_text):
    """Render code as a one-cell table with a soft background fill."""
    tbl = container.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.LEFT
    tbl.autofit = False
    cell = tbl.cell(0, 0)
    # width
    cell.width = Cm(16.0)
    # shading
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), 'F2F2F2')
    tcPr.append(shd)
    # vertical alignment
    vAlign = OxmlElement('w:vAlign')
    vAlign.set(qn('w:val'), 'center')
    tcPr.append(vAlign)

    # Clear default paragraph, then add code lines
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    pf.line_spacing = 1.15
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    # Remove existing run if any
    for r in list(p.runs):
        r._element.getparent().remove(r._element)
    first = True
    for line in code_text.splitlines():
        if first:
            run = p.add_run(line)
            first = False
        else:
            new_p = cell.add_paragraph()
            new_p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            npf = new_p.paragraph_format
            npf.space_before = Pt(0)
            npf.space_after = Pt(0)
            npf.line_spacing = 1.15
            npf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
            run = new_p.add_run(line)
        rPr = run._element.get_or_add_rPr()
        rFonts = OxmlElement('w:rFonts')
        rFonts.set(qn('w:ascii'), 'Consolas')
        rFonts.set(qn('w:hAnsi'), 'Consolas')
        rFonts.set(qn('w:cs'), 'Consolas')
        rPr.append(rFonts)
        run.font.size = Pt(11)
    # spacer paragraph
    spacer = container.add_paragraph()
    spacer.paragraph_format.space_before = Pt(0)
    spacer.paragraph_format.space_after = Pt(0)
    spacer.paragraph_format.line_spacing = 1.0
    return tbl


def set_table_borders(tbl):
    tblPr = tbl._tbl.tblPr
    borders = OxmlElement('w:tblBorders')
    for border_name in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'):
        border = OxmlElement(f'w:{border_name}')
        border.set(qn('w:val'), 'single')
        border.set(qn('w:sz'), '6')
        border.set(qn('w:space'), '0')
        border.set(qn('w:color'), '7F7F7F')
        borders.append(border)
    tblPr.append(borders)


def shade_cell(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), fill_hex)
    tcPr.append(shd)


def add_table_caption(container, text):
    """Add a caption ABOVE the table (caption = table name in this report)."""
    p = container.add_paragraph()
    run = p.add_run(text)
    set_run_font(run, bold=True, italic=True, size=12)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf = p.paragraph_format
    pf.space_before = Pt(8)
    pf.space_after = Pt(4)
    pf.line_spacing = 1.3
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    pf.first_line_indent = Cm(0)
    pf.keep_with_next = True
    return p


def add_table(container, header_row, data_rows, *, col_widths=None, header_fill="DDEBF7"):
    """Build a table with header + data rows; header bold + shaded."""
    if not data_rows:
        return None
    cols = len(header_row)
    tbl = container.add_table(rows=1 + len(data_rows), cols=cols)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False if col_widths else True

    # header
    hdr = tbl.rows[0]
    for j, htext in enumerate(header_row):
        cell = hdr.cells[j]
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        pf = p.paragraph_format
        pf.space_before = Pt(2)
        pf.space_after = Pt(2)
        pf.line_spacing = 1.15
        pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
        pf.first_line_indent = Cm(0)
        run = p.add_run(htext)
        set_run_font(run, bold=True, size=12)
        shade_cell(cell, header_fill)
        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        if col_widths:
            cell.width = col_widths[j]

    # data
    for i, row in enumerate(data_rows, start=1):
        tr = tbl.rows[i]
        for j, val in enumerate(row):
            cell = tr.cells[j]
            cell.text = ""
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            pf = p.paragraph_format
            pf.space_before = Pt(2)
            pf.space_after = Pt(2)
            pf.line_spacing = 1.15
            pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
            pf.first_line_indent = Cm(0)
            run = p.add_run(str(val))
            set_run_font(run, size=12)
            cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
            if col_widths:
                cell.width = col_widths[j]
    set_table_borders(tbl)
    return tbl


def add_table_text_block(container):
    """Small spacer paragraph after a table."""
    p = container.add_paragraph()
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    return p


def add_figure_caption(container, text):
    """Add a caption BELOW the figure (in this report, used as figure labels)."""
    p = container.add_paragraph()
    run = p.add_run(text)
    set_run_font(run, bold=True, italic=True, size=12)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf = p.paragraph_format
    pf.space_before = Pt(2)
    pf.space_after = Pt(10)
    pf.line_spacing = 1.3
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    pf.first_line_indent = Cm(0)
    return p


def add_simple_figure_block(container, ascii_lines, caption_text):
    """Render a simple ASCII-style architecture diagram inside a bordered table
    so it scales cleanly when printed in A4, with a caption below."""
    body = "\n".join(ascii_lines)
    tbl = container.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    cell = tbl.cell(0, 0)
    cell.width = Cm(16.0)
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), 'FFFFFF')
    tcPr.append(shd)
    vAlign = OxmlElement('w:vAlign')
    vAlign.set(qn('w:val'), 'center')
    tcPr.append(vAlign)

    # fill paragraph
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    pf.line_spacing = 1.15
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    for r in list(p.runs):
        r._element.getparent().remove(r._element)

    first = True
    for line in ascii_lines:
        if first:
            run = p.add_run(line)
            first = False
        else:
            new_p = cell.add_paragraph()
            new_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            npf = new_p.paragraph_format
            npf.space_before = Pt(0)
            npf.space_after = Pt(0)
            npf.line_spacing = 1.15
            npf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
            run = new_p.add_run(line)
        rPr = run._element.get_or_add_rPr()
        rFonts = OxmlElement('w:rFonts')
        rFonts.set(qn('w:ascii'), 'Consolas')
        rFonts.set(qn('w:hAnsi'), 'Consolas')
        rFonts.set(qn('w:cs'), 'Consolas')
        rPr.append(rFonts)
        run.font.size = Pt(11)
    set_table_borders(tbl)
    add_figure_caption(container, caption_text)
    return tbl


# ---------------------------------------------------------------------------
# Page setup (margins)
# ---------------------------------------------------------------------------

def setup_document(doc):
    section = doc.sections[0]
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.17)
    section.right_margin = Cm(2.0)
    # Default style
    style = doc.styles['Normal']
    style.font.name = 'Times New Roman'
    style.font.size = Pt(13)
    rpr = style.element.get_or_add_rPr()
    rFonts = rpr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = OxmlElement('w:rFonts')
        rpr.append(rFonts)
    rFonts.set(qn('w:ascii'), 'Times New Roman')
    rFonts.set(qn('w:hAnsi'), 'Times New Roman')
    rFonts.set(qn('w:cs'), 'Times New Roman')
    rFonts.set(qn('w:eastAsia'), 'Times New Roman')


# ---------------------------------------------------------------------------
# Section writers
# ---------------------------------------------------------------------------

def write_2_1(doc):
    add_heading(doc, "2.1. Phân tích yêu cầu bài toán", level=2)

    add_paragraph(doc,
        "Hệ thống được xây dựng theo mô hình sàn thương mại điện tử đa nhà bán (multi-vendor marketplace), "
        "trong đó có bốn nhóm người dùng chính gồm Customer, Vendor, Moderator và Admin. "
        "Mỗi nhóm được cung cấp chức năng riêng dựa trên vai trò tài khoản và các quy tắc phân quyền "
        "được cấu hình trong Spring Security. Toàn bộ phần này được khảo sát trực tiếp từ source code "
        "hiện tại của project: package, Controller, Service, ServiceImpl, Document, Enum và "
        "SecurityConfig của dự án CNJ70.")

    add_heading(doc, "2.1.1. Phân tích chức năng theo nhóm người dùng", level=3)

    # a) Customer
    add_letter_item(doc, "a", "Customer")
    add_bullet(doc, "Đăng ký, đăng nhập và đăng xuất", level=0)
    add_bullet(doc, "Người dùng đăng ký tại /auth/register bằng email, password, họ tên, số điện thoại, địa chỉ. "
                    "Hệ thống kiểm tra email đã tồn tại, mã hóa mật khẩu bằng BCrypt và lưu tài khoản với "
                    "trạng thái ACTIVE. Việc đăng ký tài khoản ADMIN hoặc MODERATOR từ giao diện public bị từ chối "
                    "(AuthServiceImpl.register).", level=1)
    add_bullet(doc, "Khi đăng nhập tại /auth/login, hệ thống tìm User theo email, kiểm tra mật khẩu và trạng thái "
                    "tài khoản. Nếu hợp lệ, hệ thống tạo JWT chứa subject, userId và role, đặt JWT vào cookie tên "
                    "jwt (cùng với Bearer header) và chuyển hướng theo vai trò. Khi đăng xuất /auth/logout, cookie "
                    "jwt được xóa.", level=1)
    add_bullet(doc, "Xem và tìm kiếm sản phẩm", level=0)
    add_bullet(doc, "Customer truy cập /products để xem danh sách sản phẩm, tìm theo tên, lọc theo danh mục và "
                    "sắp xếp theo giá, đánh giá hoặc mới nhất. Tại /products/{id} hiển thị chi tiết sản phẩm, "
                    "danh sách đánh giá và các sản phẩm liên quan.", level=1)
    add_bullet(doc, "Xem shop", level=0)
    add_bullet(doc, "Customer truy cập /shop/{id} để xem trang shop công khai: thông tin shop, sản phẩm theo danh "
                    "mục, khoảng giá, các sản phẩm nổi bật, đánh giá gần đây và phân trang.", level=1)
    add_bullet(doc, "Quản lý giỏ hàng", level=0)
    add_bullet(doc, "Tại /cart, hệ thống nhóm sản phẩm theo shop và cho phép thêm, cập nhật số lượng, xóa sản phẩm. "
                    "Khi thêm vào giỏ, endpoint /api/cart/add kiểm tra số lượng không vượt quá tồn kho của sản phẩm. "
                    "Mã voucher đã áp dụng được lưu trực tiếp trên Document Cart để giữ xuyên suốt phiên.", level=1)
    add_bullet(doc, "Checkout và đặt hàng", level=0)
    add_bullet(doc, "Customer thực hiện checkout tại /checkout: nhập số điện thoại, địa chỉ nhận hàng và phương thức "
                    "thanh toán (mặc định COD). Hệ thống kiểm tra sản phẩm, tồn kho, tính tiền hàng, phí vận chuyển "
                    "(15.000 VND cố định) và tổng tiền, sau đó tạo Order, trừ tồn kho và dọn giỏ hàng trong cùng "
                    "transaction.", level=1)
    add_bullet(doc, "Voucher", level=0)
    add_bullet(doc, "Customer xem voucher WEB khả dụng tại /vouchers và áp dụng voucher qua /checkout/apply-voucher. "
                    "Hệ thống kiểm tra trạng thái active, thời hạn, lượt dùng, đối tượng shop/sản phẩm trước khi tính "
                    "khoản giảm.", level=1)
    add_bullet(doc, "Theo dõi đơn hàng", level=0)
    add_bullet(doc, "Customer xem lịch sử đơn hàng tại /orders và xem chi tiết từng đơn tại /orders/{id}.", level=1)
    add_bullet(doc, "Đánh giá sản phẩm", level=0)
    add_bullet(doc, "Sau khi nhận hàng (Order DELIVERED), Customer có thể tạo review tại "
                    "/products/{productId}/reviews; rating từ 1 đến 5, nội dung không rỗng và mỗi user chỉ review "
                    "một lần cho một sản phẩm. Sau khi lưu, hệ thống cập nhật rating trung bình và reviewCount trên "
                    "Product.", level=1)

    # b) Vendor/Shop
    add_letter_item(doc, "b", "Vendor/Shop")
    add_bullet(doc, "Dashboard: Vendor truy cập /vendor/dashboard để xem thống kê doanh thu và các sản phẩm nổi bật.", level=0)
    add_bullet(doc, "Quản lý shop: Vendor tạo shop tại /vendor/shop/create và cập nhật tại /vendor/shop/update. "
                    "Shop mới có trạng thái PENDING và chỉ hoạt động khi Admin duyệt theo ShopStatus.", level=0)
    add_bullet(doc, "Quản lý sản phẩm: Vendor xem danh sách sản phẩm tại /vendor/products, tạo tại "
                    "/vendor/products/create, chỉnh sửa và xóa. Form sản phẩm hỗ trợ specifications, variants và "
                    "upload ảnh. Mỗi Product mới được đưa vào hàng chờ Moderation với moderationStatus mặc định "
                    "PENDING_MANUAL.", level=0)
    add_bullet(doc, "Quản lý đơn hàng: Vendor xem các đơn liên quan đến shop tại /vendor/orders. Khi cập nhật "
                    "trạng thái, hệ thống kiểm tra quyền sở hữu shop trước khi thay đổi. Khi chuyển sang DELIVERED, "
                    "hệ thống ghi nhận deliveredAt.", level=0)
    add_bullet(doc, "Quản lý voucher SHOP: Vendor tạo, sửa, xóa và kích hoạt/vô hiệu hóa voucher SHOP cho chính "
                    "shop mình tại /vendor/vouchers. Voucher SHOP luôn gắn shopId và shopName.", level=0)
    add_bullet(doc, "Profile: Vendor xem thông tin tài khoản và shop tại /vendor/profile.", level=0)

    # c) Admin
    add_letter_item(doc, "c", "Admin")
    add_bullet(doc, "Dashboard: Admin truy cập /admin/dashboard xem thống kê tổng quan và doanh thu theo các "
                    "khoảng DAY, WEEK, MONTH, QUARTER, YEAR, ALL.", level=0)
    add_bullet(doc, "Quản lý người dùng: Admin xem danh sách, tìm kiếm, lọc theo role/status và khóa/mở khóa tài "
                    "khoản tại /admin/users.", level=0)
    add_bullet(doc, "Quản lý shop: Admin xem danh sách và chi tiết shop, thực hiện approve, activate, deactivate "
                    "và reject tại /admin/shops.", level=0)
    add_bullet(doc, "Quản lý danh mục: Admin CRUD danh mục tại /admin/categories, lọc theo trạng thái active. "
                    "Khi xóa danh mục có kiểm tra ràng buộc sản phẩm còn liên quan.", level=0)
    add_bullet(doc, "Quản lý đơn hàng: Admin xem danh sách và chi tiết đơn hàng tại /admin/orders (read-only, "
                    "không có endpoint cập nhật trạng thái).", level=0)
    add_bullet(doc, "Quản lý sản phẩm: Admin xem, tìm kiếm, lọc sản phẩm; thực hiện hide, unhide và delete qua "
                    "/admin/products.", level=0)
    add_bullet(doc, "Quản lý review: Admin xem danh sách, tìm kiếm, lọc theo rating và xóa review tại "
                    "/admin/reviews.", level=0)
    add_bullet(doc, "Quản lý banner: Admin tạo, sửa, xóa và publish/unpublish banner tại /admin/banners.", level=0)
    add_bullet(doc, "Quản lý voucher WEB: Admin quản lý voucher WEB tại /admin/vouchers: tạo, sửa, xóa, activate, "
                    "deactivate. Admin không thao tác trên voucher SHOP.", level=0)

    # d) Moderator
    add_letter_item(doc, "d", "Moderator")
    add_bullet(doc, "Hàng chờ kiểm duyệt: Moderator xem và xử lý ReportCase, Review bị report tại /moderator.", level=0)
    add_bullet(doc, "Quyết định: Approve/Reject/Escalate các case vi phạm thuộc report_cases, đồng thời thực hiện "
                    "ẩn/khôi phục review vi phạm.", level=0)
    add_bullet(doc, "Hành vi: Ẩn review (reviewService.hideReview), khôi phục review (reviewService.restoreReview). "
                    "Mọi quyết định kèm AuditLog với mức severity phù hợp.", level=0)

    # 2.1.2 Phân quyền truy cập
    add_heading(doc, "2.1.2. Phân quyền truy cập", level=3)
    add_paragraph(doc,
        "Phân quyền được cấu hình tập trung trong SecurityConfig với các antMatcher rõ ràng cho từng nhóm URL. "
        "Các role chính là ADMIN, VENDOR, CUSTOMER và MODERATOR.")

    add_table_caption(doc, "Bảng 2.1. Phân quyền URL chính theo cấu hình SecurityConfig")
    add_table(
        doc,
        header_row=["Nhóm URL", "Quyền truy cập", "Ghi chú"],
        data_rows=[
            ("/", "PUBLIC", "ViewController redirect sang /home"),
            ("/home", "PUBLIC", "Trang chủ"),
            ("/auth/**", "PUBLIC", "Login, register, logout"),
            ("/products/**", "PUBLIC", "Danh sách, chi tiết, review GET"),
            ("/vouchers", "PUBLIC", "Trang voucher WEB"),
            ("/cart/count", "PUBLIC", "Đếm sản phẩm trong giỏ"),
            ("/vendor/**", "ROLE_VENDOR", "Trung tâm quản lý shop"),
            ("/admin/**", "ROLE_ADMIN hoặc ROLE_MODERATOR", "Endpoint chia sẻ cho admin/moderator"),
            ("/moderator/**", "ROLE_MODERATOR", "Hàng chờ kiểm duyệt"),
            ("/cart/**", "Authenticated", "GET/POST giỏ hàng"),
            ("/checkout/**", "Authenticated", "Trang checkout"),
            ("/orders/**", "Authenticated", "Lịch sử đơn hàng"),
            ("/complaints/**", "Authenticated", "Customer khiếu nại"),
            ("/api/**", "Authenticated", "API REST dùng chung"),
            ("Mọi request khác", "Authenticated", "Mặc định phải đăng nhập"),
        ],
        col_widths=[Cm(3.8), Cm(5.5), Cm(7.0)],
    )
    add_table_text_block(doc)

    # 2.1.3 Bảng yêu cầu chức năng
    add_heading(doc, "2.1.3. Bảng yêu cầu chức năng", level=3)
    add_paragraph(doc,
        "Bảng dưới tổng hợp các yêu cầu chức năng chính đã được triển khai, kèm actor và endpoint tương ứng "
        "trong source code.")

    add_table_caption(doc, "Bảng 2.2. Yêu cầu chức năng theo actor và endpoint")
    add_table(
        doc,
        header_row=["Mã", "Yêu cầu chức năng", "Actor", "Endpoint/URL chính"],
        data_rows=[
            ("FR-01", "Đăng ký tài khoản", "Customer", "/auth/register"),
            ("FR-02", "Đăng nhập/đăng xuất", "Tất cả", "/auth/login, /auth/logout"),
            ("FR-03", "Xem danh sách sản phẩm", "Customer", "/products"),
            ("FR-04", "Xem chi tiết sản phẩm", "Customer", "/products/{id}"),
            ("FR-05", "Xem trang shop công khai", "Customer", "/shop/{id}"),
            ("FR-06", "Thêm/cập nhật/xóa giỏ hàng", "Customer", "/api/cart/add, /cart/update, /cart/remove"),
            ("FR-07", "Áp dụng voucher", "Customer", "/api/cart/apply-voucher, /api/cart/remove-voucher"),
            ("FR-08", "Xem voucher WEB khả dụng", "Customer", "/vouchers"),
            ("FR-09", "Checkout và đặt hàng", "Customer", "/checkout, /checkout/place-order"),
            ("FR-10", "Theo dõi đơn hàng", "Customer", "/orders, /orders/{id}"),
            ("FR-11", "Đánh giá sản phẩm", "Customer", "/products/{id}/reviews"),
            ("FR-12", "Quản lý shop", "Vendor", "/vendor/shop/create, /vendor/shop/update"),
            ("FR-13", "Quản lý sản phẩm của shop", "Vendor", "/vendor/products"),
            ("FR-14", "Quản lý đơn hàng của shop", "Vendor", "/vendor/orders"),
            ("FR-15", "Quản lý voucher SHOP", "Vendor", "/vendor/vouchers"),
            ("FR-16", "Dashboard vendor", "Vendor", "/vendor/dashboard, /vendor/profile"),
            ("FR-17", "Quản lý người dùng", "Admin", "/admin/users"),
            ("FR-18", "Duyệt/khóa shop", "Admin", "/admin/shops"),
            ("FR-19", "Quản lý danh mục", "Admin", "/admin/categories"),
            ("FR-20", "Quản lý đơn hàng (read-only)", "Admin", "/admin/orders"),
            ("FR-21", "Ẩn/hiện/xóa sản phẩm", "Admin", "/admin/products"),
            ("FR-22", "Quản lý review", "Admin", "/admin/reviews"),
            ("FR-23", "Quản lý banner", "Admin", "/admin/banners"),
            ("FR-24", "Quản lý voucher WEB", "Admin", "/admin/vouchers"),
            ("FR-25", "Xử lý ReportCase", "Moderator", "/moderator"),
            ("FR-26", "Ẩn/khôi phục review", "Moderator", "/moderator"),
        ],
        col_widths=[Cm(1.5), Cm(5.8), Cm(2.4), Cm(6.5)],
    )
    add_table_text_block(doc)

    # 2.1.4 Yêu cầu phi chức năng
    add_heading(doc, "2.1.4. Yêu cầu phi chức năng", level=3)
    add_bullet(doc, "Bảo mật mật khẩu: Mật khẩu được mã hóa bằng BCrypt thông qua BCryptPasswordEncoder trong SecurityConfig.", level=0)
    add_bullet(doc, "Xác thực: Sử dụng JWT với thời hạn 86.400.000 ms tương đương 24 giờ (jwt.expiration trong application.yml).", level=0)
    add_bullet(doc, "Phân quyền: Cấu hình tập trung qua SecurityFilterChain, các request phải xác thực trước khi vào Controller.", level=0)
    add_bullet(doc, "Mã hóa PII: Các trường CCCD, mã số thuế, số tài khoản ngân hàng trên User và Shop được mã hóa AES-256-GCM.", level=0)
    add_bullet(doc, "Tính sẵn sàng: Ứng dụng chạy tại port 8081 (server.port) với context-path mặc định rỗng, kết nối MongoDB database cnj70_ecommerce.", level=0)
    add_bullet(doc, "Lưu trữ thời gian: MongoConfig bật @EnableMongoAuditing nên các trường @CreatedDate và @LastModifiedDate được tự động cập nhật.", level=0)
    add_bullet(doc, "Khả năng quan sát: AuditLog (collection audit_logs) ghi nhận các sự kiện tuân thủ (moderation, KYC, escalation, login...) với actor, action, resource, severity.", level=0)
    add_bullet(doc, "Xử lý ngoại lệ tập trung: GlobalExceptionHandler chuyển các loại exception nghiệp vụ sang trang error tương ứng (404/400/403/409/500).", level=0)
    add_bullet(doc, "Cấu hình môi trường: dotenv-java đọc biến môi trường từ file .env, đảm bảo không hard-code secret trong source.", level=0)
    add_bullet(doc, "Frontend: Thymeleaf kết hợp Tailwind CSS, Font Awesome, Google Fonts Work Sans, responsive trên desktop và mobile.", level=0)


def write_2_2(doc):
    add_heading(doc, "2.2. Kiến trúc hệ thống", level=2)

    add_heading(doc, "2.2.1. Kiến trúc tổng thể", level=3)
    add_paragraph(doc,
        "Project được tổ chức theo kiến trúc phân lớp (layered architecture) gồm "
        "Controller → Service → ServiceImpl → Repository → Document → MongoDB, "
        "kết hợp với các thành phần bảo mật, xử lý ngoại lệ, interceptor và cấu hình mở rộng. "
        "Phía client, các view Thymeleaf được render server-side và trả về cho trình duyệt.")

    ascii_flow = [
        "+----------------+      +-----------------------+      +-----------------+",
        "|    Browser     | ---> | Spring Security       | ---> | DispatcherServlet|",
        "| (HTML/JS/CSS)  |      | Filter Chain + JWT    |      |  (Controller)   |",
        "+----------------+      +-----------------------+      +-----------------+",
        "                                                            |        |",
        "                                                            v        v",
        "                                                       +---------------+",
        "                                                       | Service Impl  |",
        "                                                       +---------------+",
        "                                                            |",
        "                                                            v",
        "                                                       +---------------+",
        "                                                       | Repository    |",
        "                                                       | MongoTemplate |",
        "                                                       +---------------+",
        "                                                            |",
        "                                                            v",
        "                                                       +---------------+",
        "                                                       |   MongoDB     |",
        "                                                       | (Documents)   |",
        "                                                       +---------------+",
        "                                                            |",
        "                                                            v",
        "                                                       +---------------+",
        "                                                       | Thymeleaf/JSON|",
        "                                                       |   Response    |",
        "                                                       +---------------+",
    ]
    add_simple_figure_block(doc, ascii_flow, "Hình 2.1. Luồng xử lý request tổng quát của hệ thống")

    add_paragraph(doc,
        "Trong đó, các thành phần có vai trò chính như sau:")

    add_bullet(doc, "Controller: Tiếp nhận HTTP request, kiểm tra ràng buộc đầu vào, gọi Service tương ứng và trả về view Thymeleaf hoặc ResponseEntity.", level=0)
    add_bullet(doc, "Service / ServiceImpl: Chứa logic nghiệp vụ chính (kiểm tra điều kiện, tính toán tiền, trừ tồn kho, tạo AuditLog...).", level=0)
    add_bullet(doc, "Repository: Sử dụng MongoRepository và MongoTemplate để truy cập collection MongoDB, định nghĩa các finder theo trường dữ liệu.", level=0)
    add_bullet(doc, "Document: Mỗi collection MongoDB được ánh xạ qua một class @Document, kèm index trên các trường thường truy vấn.", level=0)
    add_bullet(doc, "Spring Security: Bộ lọc JwtAuthenticationFilter đọc JWT từ cookie jwt hoặc Authorization: Bearer header, thiết lập SecurityContext và áp dụng các quy tắc phân quyền.", level=0)
    add_bullet(doc, "GlobalExceptionHandler: Bắt các exception nghiệp vụ (ResourceNotFoundException, BadRequestException, BusinessException, UnauthorizedException, ConflictException, AccessDeniedException, NoResourceFoundException, MethodArgumentNotValidException) và chuyển sang trang error tương ứng.", level=0)
    add_bullet(doc, "CartCountInterceptor và GlobalControllerAdvice: Bổ sung cartCount, hasShop, kycStatus, activeViolations, availableVoucherCount cho mọi view.", level=0)
    add_bullet(doc, "WebMvcConfig: Đăng ký view controller /, ánh xạ /uploads/** với thư mục static và đăng ký CartCountInterceptor.", level=0)

    # 2.2.2 Thành phần Security
    add_heading(doc, "2.2.2. Thành phần Security", level=3)
    add_paragraph(doc,
        "Cấu hình Security được tập trung tại SecurityConfig với bean SecurityFilterChain. Hệ thống "
        "sử dụng BCryptPasswordEncoder để mã hóa mật khẩu; đăng ký JwtAuthenticationFilter trước "
        "UsernamePasswordAuthenticationFilter; tắt CSRF cho các endpoint phù hợp; thiết lập chính sách "
        "phiên STATELESS; cấu hình entry point và access denied handler trả về trang lỗi.")

    add_table_caption(doc, "Bảng 2.3. Các thành phần chính của SecurityConfig")
    add_table(
        doc,
        header_row=["Thành phần", "Vai trò"],
        data_rows=[
            ("BCryptPasswordEncoder", "Mã hóa và xác thực mật khẩu người dùng."),
            ("JwtAuthenticationFilter", "Đọc JWT từ cookie hoặc header, tạo UsernamePasswordAuthenticationToken và đưa vào SecurityContext."),
            ("CustomUserDetailsService", "Nạp User từ UserRepository theo username cho Spring Security."),
            ("CustomUserDetails", "Đối tượng principal chứa id, email, fullName, role, shopId, status..."),
            ("AuthenticationEntryPoint", "Trả trang lỗi khi chưa đăng nhập mà truy cập tài nguyên bảo vệ."),
            ("AccessDeniedHandler", "Trả trang lỗi 403 khi đã đăng nhập nhưng không đủ quyền."),
            ("SessionCreationPolicy.STATELESS", "Không lưu session phía server, mọi request đều đi kèm JWT."),
        ],
        col_widths=[Cm(5.5), Cm(11.0)],
    )
    add_table_text_block(doc)

    add_paragraph(doc,
        "JWT được ký bằng thuật toán HS256 với secret nạp từ biến môi trường jwt.secret. Payload chứa "
        "subject (username), userId và role. Thời hạn hiệu lực là 24 giờ. Khi User bị đổi trạng thái "
        "không còn ACTIVE, các request tiếp theo với JWT cũ vẫn bị từ chối vì logic nghiệp vụ kiểm tra lại "
        "trạng thái.")

    # 2.2.3 Mô tả lớp
    add_heading(doc, "2.2.3. Mô tả lớp", level=3)
    add_paragraph(doc, "Các package Java chính của project:")

    add_table_caption(doc, "Bảng 2.4. Mô tả các package chính")
    add_table(
        doc,
        header_row=["Package", "Vai trò"],
        data_rows=[
            ("config", "Cấu hình Spring (Security, MongoDB, Web MVC, Controller Advice, Audit)."),
            ("controller", "Controller web theo actor: web, auth, vendor, admin, moderator, api."),
            ("controller.api", "Các REST controller dùng chung cho AJAX."),
            ("document", "Các Document ánh xạ sang collection MongoDB (User, Shop, Product, Order, Cart, Voucher, Review, Banner...)."),
            ("dto.request / dto.response", "Các lớp DTO cho form nhập và dữ liệu trả về."),
            ("enums", "Các enum miền nghiệp vụ: UserRole, AccountStatus, ProductStatus, OrderStatus, VoucherType, DiscountType, KycStatus, ShopStatus, BannerStatus..."),
            ("exception", "Các exception nghiệp vụ và GlobalExceptionHandler xử lý tập trung."),
            ("interceptor", "CartCountInterceptor bổ sung cartCount cho view."),
            ("repository", "Các interface MongoRepository và truy vấn MongoTemplate tuỳ biến."),
            ("security", "JwtAuthenticationFilter, CustomUserDetails, CustomUserDetailsService, JwtService."),
            ("service / service.impl", "Interface Service và ServiceImpl chứa toàn bộ logic nghiệp vụ."),
            ("util", "Các tiện ích dùng chung (chuẩn hoá URL, parse enum, build DTO...)."),
            ("scheduler", "Các tác vụ nền (escalation, idempotency)."),
        ],
        col_widths=[Cm(5.5), Cm(11.0)],
    )
    add_table_text_block(doc)

    # 2.2.4 Controller
    add_heading(doc, "2.2.4. Controller", level=3)
    add_paragraph(doc,
        "Các Controller được phân theo actor và nhóm chức năng. Mỗi Controller đều được đăng ký với "
        "@RequestMapping ở cấp class, các method xử lý cụ thể với @GetMapping/@PostMapping.")

    add_table_caption(doc, "Bảng 2.5. Danh sách Controller theo nhóm")
    add_table(
        doc,
        header_row=["Controller", "Package", "URL cơ sở"],
        data_rows=[
            ("AuthController", "controller.auth", "/auth"),
            ("HomeController", "controller.web", "/home"),
            ("ProductController", "controller.web", "/products"),
            ("ShopController", "controller.web", "/shop"),
            ("CartController", "controller.web", "/cart, /api/cart"),
            ("OrderController", "controller.web", "/checkout, /orders"),
            ("ReviewController", "controller.web", "/products/{id}/reviews, /reviews, /api/reviews"),
            ("VoucherController", "controller", "/vouchers, /vendor/vouchers, /admin/vouchers, /checkout/apply-voucher"),
            ("VendorDashboardController, VendorShopController, VendorProductController, VendorOrderController, VendorProfileController", "controller.vendor", "/vendor"),
            ("AdminDashboardController, AdminUserController, AdminShopController, AdminProductController, AdminOrderController, AdminCategoryController, AdminBannerController, AdminReviewController", "controller.admin", "/admin"),
            ("ModeratorController, ModeratorReportCaseController, ModeratorReviewController", "controller.moderator", "/moderator"),
            ("ComplaintController, ReturnController, RefundController", "controller", "/complaints, /returns, /refunds"),
            ("FileUploadController", "controller", "/api/upload"),
            ("KycController", "controller", "/kyc, /vendor/kyc, /admin/kyc"),
            ("LegalDocumentController", "controller", "/legal"),
            ("EscalationController, ViolationController, AuditLogController", "controller", "/moderator, /admin"),
        ],
        col_widths=[Cm(8.5), Cm(3.5), Cm(4.5)],
    )
    add_table_text_block(doc)

    # 2.2.5 Service / ServiceImpl
    add_heading(doc, "2.2.5. Service và ServiceImpl", level=3)
    add_paragraph(doc,
        "Mỗi nhóm nghiệp vụ có một interface Service và một ServiceImpl tương ứng. ServiceImpl sử dụng "
        "@RequiredArgsConstructor (Lombok) để inject các Repository và Service phụ thuộc.")

    add_table_caption(doc, "Bảng 2.6. Các Service và ServiceImpl chính")
    add_table(
        doc,
        header_row=["Service", "ServiceImpl", "Phạm vi nghiệp vụ"],
        data_rows=[
            ("AuthService", "AuthServiceImpl", "Đăng ký, đăng nhập, kiểm tra email, ghi AuditLog."),
            ("ProductService", "ProductServiceImpl", "CRUD sản phẩm vendor, moderation queue, Auto Moderation, ownership guard."),
            ("CartService", "CartServiceImpl", "Quản lý giỏ hàng: thêm, cập nhật, xóa, applied voucher."),
            ("OrderService", "OrderServiceImpl", "Tạo đơn hàng, trừ tồn kho, áp voucher, cập nhật vận chuyển."),
            ("ReviewService", "ReviewServiceImpl", "Review, hasUserReviewedProduct, canUserReviewProduct, hide/restore, report."),
            ("VoucherService", "VoucherServiceImpl", "Tạo/sửa voucher SHOP và WEB, validateForCheckout, tryIncrement/tryDecrement."),
            ("VoucherApplicationGateway", "VoucherApplicationGatewayImpl", "Reserve / release voucher atomic."),
            ("ShopService / VendorService", "VendorServiceImpl", "Shop của vendor, dashboard, KYC."),
            ("AdminProductService, AdminShopService, AdminUserService, AdminOrderService, AdminBannerService, AdminCategoryService, AdminReviewService, AdminVoucherService, AdminKycService", "*ServiceImpl", "Tác vụ quản trị tương ứng."),
            ("ModeratorService, ModeratorReportCaseService, ModeratorReviewService", "*ServiceImpl", "Hàng chờ kiểm duyệt và quyết định."),
            ("KycService", "KycServiceImpl", "Submit, third-party verify, admin duyệt."),
            ("AuditLogService", "AuditLogServiceImpl", "Ghi AuditLog theo severity."),
            ("ComplaintService", "ComplaintServiceImpl", "Khiếu nại 3 cấp."),
            ("ReturnService, RefundService", "*ServiceImpl", "Return/Refund request contract."),
            ("BannerService, CategoryService, NotificationService, LegalDocumentService", "*ServiceImpl", "Chức năng phụ trợ."),
        ],
        col_widths=[Cm(5.0), Cm(5.0), Cm(6.5)],
    )
    add_table_text_block(doc)

    # 2.2.6 Repository
    add_heading(doc, "2.2.6. Repository", level=3)
    add_paragraph(doc,
        "Các Repository đều kế thừa MongoRepository<T, String> của Spring Data MongoDB, kết hợp với "
        "MongoTemplate trong các ServiceImpl khi cần truy vấn tuỳ biến (ví dụ: lọc Voucher type WEB, "
        "ẩn/hiện sản phẩm atomic).")

    add_table_caption(doc, "Bảng 2.7. Danh sách Repository")
    add_table(
        doc,
        header_row=["Repository", "Collection", "Finder tiêu biểu"],
        data_rows=[
            ("UserRepository", "users", "findByEmail, existsByEmail"),
            ("ShopRepository", "shops", "findByOwnerId"),
            ("ProductRepository", "products", "findByShopIdAndStatus, findByCategoryId, findByNameContainingIgnoreCase, findTop10ByStatusOrderByCreatedAtDesc"),
            ("CategoryRepository", "categories", "findByActiveTrue"),
            ("OrderRepository", "orders", "findByUserId, findByShopIdOrderByCreatedAtDesc"),
            ("CartRepository", "carts", "findByUserId"),
            ("VoucherRepository", "vouchers", "findByCode, existsByCode, findByShopId, findByShopIdAndActiveTrue"),
            ("ReviewRepository", "reviews", "findByProductId, findByUserId, findByProductIdAndUserId, findByProductIdOrderByCreatedAtDesc, findByModerationStatus, countByProductId, findByProductIdInOrderByCreatedAtDesc"),
            ("KycProfileRepository", "kyc_profiles", "findByUserId"),
            ("AuditLogRepository", "audit_logs", "Tìm theo actor, action, resource..."),
            ("BannerRepository, ModerationHistoryRepository, ComplaintRepository, EscalationRepository, ReportCaseRepository, ReturnRequestRepository, RefundRequestRepository, ViolationRepository, AuditLogEntryRepository, LegalDocumentRepository, SchedulerIdempotencyRepository", "Tương ứng", "Finder theo các trường @Indexed."),
        ],
        col_widths=[Cm(6.0), Cm(3.5), Cm(7.0)],
    )
    add_table_text_block(doc)

    # 2.2.7 Document
    add_heading(doc, "2.2.7. Document/Entity", level=3)
    add_paragraph(doc,
        "Mỗi collection MongoDB được ánh xạ bằng một class trong package document, dùng annotation "
        "@Document(collection = \"...\") của Spring Data MongoDB. Trường @Id là primary key, các trường "
        "hay truy vấn được đánh @Indexed. Trường createdAt và updatedAt do @EnableMongoAuditing tự sinh.")

    add_table_caption(doc, "Bảng 2.8. Các Document chính và collection tương ứng")
    add_table(
        doc,
        header_row=["Document", "Collection", "Vai trò"],
        data_rows=[
            ("User", "users", "Tài khoản người dùng."),
            ("Shop", "shops", "Cửa hàng của vendor."),
            ("Product", "products", "Sản phẩm."),
            ("Category", "categories", "Danh mục sản phẩm."),
            ("Order", "orders", "Đơn hàng, OrderItem, SubOrderShipping."),
            ("Cart", "carts", "Giỏ hàng, CartItem."),
            ("Voucher", "vouchers", "Mã giảm giá (SHOP/WEB)."),
            ("Review", "reviews", "Đánh giá sản phẩm."),
            ("Banner", "banners", "Banner quảng cáo."),
            ("KycProfile", "kyc_profiles", "Hồ sơ xác minh vendor."),
            ("AuditLog, AuditLogEntry", "audit_logs", "Nhật ký sự kiện."),
            ("ReportCase", "report_cases", "Phiên kiểm duyệt."),
            ("ModerationHistory", "moderation_history", "Lịch sử kiểm duyệt sản phẩm."),
            ("Escalation", "escalations", "Phiếu leo thang sang admin."),
            ("Complaint", "complaints", "Khiếu nại customer ↔ vendor, 3 cấp."),
            ("ReturnRequest", "returns", "Yêu cầu hoàn trả."),
            ("RefundRequest", "refunds", "Yêu cầu hoàn tiền."),
            ("Violation", "violations", "Vi phạm của shop."),
            ("LegalDocument", "legal_documents", "Tài liệu pháp lý."),
            ("ProductSpecification, ProductVariant", "Nhúng trong Product", "Thông số và biến thể sản phẩm."),
            ("FlashSaleStat", "Không persist", "View-model dùng cho trang chủ."),
            ("SchedulerIdempotencyRecord", "scheduler_idempotency", "Bảo đảm idempotent cho scheduler."),
        ],
        col_widths=[Cm(5.5), Cm(3.8), Cm(7.2)],
    )
    add_table_text_block(doc)

    # 2.2.8 DTO/Enum
    add_heading(doc, "2.2.8. DTO và Enum", level=3)
    add_paragraph(doc,
        "Các DTO trong package dto.request và dto.response đóng vai trò dữ liệu truyền giữa Controller "
        "và Service, tránh lộ cấu trúc Document ra view. Các Enum trong package enums chuẩn hoá miền "
        "giá trị nghiệp vụ:")

    add_table_caption(doc, "Bảng 2.9. Các Enum chính")
    add_table(
        doc,
        header_row=["Enum", "Giá trị tiêu biểu"],
        data_rows=[
            ("UserRole", "ADMIN, VENDOR, CUSTOMER, MODERATOR"),
            ("AccountStatus", "UNVERIFIED, ACTIVE, BANNED, LOCKED..."),
            ("ProductStatus", "DRAFT, ACTIVE, HIDDEN, OUT_OF_STOCK, ARCHIVED"),
            ("ShopStatus", "PENDING, APPROVED, REJECTED, SUSPENDED"),
            ("KycStatus", "NOT_SUBMITTED, PENDING_THIRD_PARTY, PENDING_ADMIN, APPROVED, THIRD_PARTY_REJECTED, ADMIN_REJECTED"),
            ("OrderStatus", "PENDING, PREPARING, SHIPPING, DELIVERED, CANCELLED"),
            ("ShippingStatus", "PENDING, SHIPPED, IN_TRANSIT, DELIVERED, FAILED"),
            ("PaymentMethod", "COD, BANK_TRANSFER, MOMO, VNPAY"),
            ("PaymentStatus", "UNPAID, PAID, REFUNDED"),
            ("VoucherType", "SHOP, WEB"),
            ("DiscountType", "PERCENT, AMOUNT (alias FIXED)"),
            ("ReviewModerationStatus", "VISIBLE, REPORTED, HIDDEN, DELETED"),
            ("ModerationStatus", "PENDING_MANUAL, AUTO_PASSED, AUTO_REJECTED, APPROVED, REJECTED, ESCALATED"),
            ("ModerationAction", "APPROVE, REJECT, ESCALATE, HIDE, RESTORE"),
            ("ReportCaseStatus, ReportCaseDecision, ReportCaseResourceType, ReportReason, ReportTargetType", "Trạng thái, quyết định, loại resource, lý do của ReportCase."),
            ("EscalationSeverity, ViolationSeverity", "LOW, MEDIUM, HIGH, CRITICAL..."),
            ("ViolationAction, ViolationType, ViolationStatus", "Hành động/loại/trạng thái của Violation."),
            ("ComplaintStatus, ComplaintLevel, ComplaintReason", "Trạng thái, cấp (LEVEL_0/1/2), lý do khiếu nại."),
            ("AuditAction, AuditSeverity", "Hành động audit (LOGIN_SUCCESS, REGISTER_SUCCESS, REVIEW_HIDDEN...) và mức (INFO/WARNING/CRITICAL)."),
            ("LegalDocumentType", "TERMS, PRIVACY, RETURN, SHIPPING, WARRANTY, COMPLAINT, PAYMENT, SITEMAP"),
            ("BannerStatus, BusinessType, AutoModerationStatus, AutoModerationFlag, KycProvider, RefundStatus, ReturnStatus, ViolationResourceType", "Các enum hỗ trợ còn lại."),
        ],
        col_widths=[Cm(6.5), Cm(10.0)],
    )
    add_table_text_block(doc)

    # 2.2.9 Exception / Interceptor / Config
    add_heading(doc, "2.2.9. Exception, Interceptor và Config", level=3)
    add_table_caption(doc, "Bảng 2.10. Thành phần xử lý ngoại lệ và mở rộng")
    add_table(
        doc,
        header_row=["Thành phần", "Vai trò"],
        data_rows=[
            ("ResourceNotFoundException", "404 cho tài nguyên không tồn tại."),
            ("BadRequestException", "400 cho yêu cầu không hợp lệ (validate, nghiệp vụ)."),
            ("BusinessException", "400 cho lỗi nghiệp vụ tổng quát."),
            ("UnauthorizedException", "403 khi thiếu quyền."),
            ("ConflictException", "409 khi xung đột dữ liệu."),
            ("GlobalExceptionHandler", "@ControllerAdvice xử lý tập trung các loại exception trên."),
            ("CartCountInterceptor", "HandlerInterceptor bổ sung cartCount cho các view web/fragments."),
            ("GlobalControllerAdvice", "Bổ sung hasShop, kycStatus, activeViolations, availableVoucherCount."),
            ("WebMvcConfig", "Đăng ký view /, resource handler /uploads/** và CartCountInterceptor."),
            ("MongoConfig", "@EnableMongoAuditing và DiscountTypeReadingConverter."),
            ("SecurityConfig", "SecurityFilterChain, BCryptPasswordEncoder, JwtAuthenticationFilter."),
        ],
        col_widths=[Cm(5.5), Cm(11.0)],
    )
    add_table_text_block(doc)

    # 2.2.10 Luồng xử lý điển hình
    add_heading(doc, "2.2.10. Luồng xử lý request điển hình", level=3)
    add_paragraph(doc, "Luồng POST /checkout/place-order được mô tả ngắn gọn như sau:")
    add_letter_item(doc, "a", "Trình duyệt gửi POST /checkout/place-order kèm cookie jwt hoặc Authorization: Bearer.")
    add_letter_item(doc, "b", "JwtAuthenticationFilter giải mã JWT, tạo CustomUserDetails và đặt vào SecurityContext.")
    add_letter_item(doc, "c", "SecurityFilterChain kiểm tra quy tắc phân quyền: /checkout/** yêu cầu đăng nhập.")
    add_letter_item(doc, "d", "OrderController đọc CheckoutReq, gọi OrderServiceImpl.createOrder.")
    add_letter_item(doc, "e", "OrderServiceImpl (transactional) lấy Cart, kiểm tra tồn kho từng Product, tính subtotal, áp voucher qua VoucherService.validateForCheckout, lưu Order, cập nhật tồn kho và dọn giỏ hàng.")
    add_letter_item(doc, "f", "VoucherService.tryIncrementUsed (qua VoucherApplicationGateway) tăng used atomic, hoặc release nếu rollback.")
    add_letter_item(doc, "g", "Controller chuyển hướng sang /web/orders kèm flash message.")


def write_2_6(doc):
    add_heading(doc, "2.6. Kết chương", level=2)
    add_paragraph(doc,
        "Chương 2 đã trình bày phân tích yêu cầu bài toán và kiến trúc hệ thống của sàn thương mại điện tử "
        "đa nhà bán CNJ70 dựa trên source code hiện tại.")
    add_paragraph(doc,
        "Ở mục 2.1, hệ thống được phân tích theo bốn nhóm người dùng Customer, Vendor, Admin và Moderator. "
        "Mỗi nhóm có các chức năng riêng biệt đi kèm URL tương ứng; quy tắc phân quyền URL được thống kê "
        "trong Bảng 2.1 và đối chiếu với SecurityConfig. Bảng 2.2 tổng hợp 26 yêu cầu chức năng chính đã "
        "được triển khai; mục 2.1.4 liệt kê các yêu cầu phi chức năng có bằng chứng từ source và cấu hình.")
    add_paragraph(doc,
        "Ở mục 2.2, kiến trúc phân lớp Browser → Spring Security → Controller → Service → ServiceImpl → "
        "Repository → Document → MongoDB được trình bày chi tiết cùng Hình 2.1 mô tả luồng tổng quát. "
        "Các thành phần Security (Bảng 2.3), package (Bảng 2.4), Controller (Bảng 2.5), Service/ServiceImpl "
        "(Bảng 2.6), Repository (Bảng 2.7), Document (Bảng 2.8), DTO/Enum (Bảng 2.9) và cấu hình mở rộng "
        "(Bảng 2.10) đã được tổng hợp. Luồng xử lý POST /checkout/place-order minh hoạ cách các thành phần "
        "phối hợp với nhau trong một nghiệp vụ cụ thể.")
    add_paragraph(doc,
        "Các nội dung ở chương 2 sẽ là cơ sở để chương 3 trình bày chi tiết ba tầng: tầng giao diện (mục 3.1), "
        "tầng nghiệp vụ (mục 3.2) và tầng thực thể (mục 3.3).")


def write_3_1(doc):
    add_heading(doc, "3.1. Tầng giao diện", level=2)
    add_paragraph(doc,
        "Tầng giao diện được xây dựng bằng Thymeleaf kết hợp Tailwind CSS, Font Awesome và Google Fonts "
        "(Work Sans). Mỗi giao diện là một file HTML trong thư mục src/main/resources/templates, được render "
        "server-side thông qua Controller tương ứng. Phần này tổng hợp các form/page chính theo từng nhóm "
        "người dùng, mô tả chức năng chính và liên kết với Controller xử lý phía sau.")

    add_heading(doc, "3.1.1. Giao diện Customer (public)", level=3)
    add_table_caption(doc, "Bảng 3.1. Giao diện phía Customer và Controller tương ứng")
    add_table(
        doc,
        header_row=["Giao diện", "Template", "Controller", "Chức năng chính"],
        data_rows=[
            ("Trang chủ", "web/index.html", "HomeController (/home)", "Banner slider, danh mục, flash sale, sản phẩm nổi bật, voucher, blog, FAQ."),
            ("Đăng ký", "auth/register.html", "AuthController (/auth/register)", "Form email, password, họ tên, SĐT, địa chỉ; chấp nhận Terms/Privacy."),
            ("Đăng nhập", "auth/login.html", "AuthController (/auth/login)", "Form email, password; chuyển hướng theo role."),
            ("Đăng xuất", "auth/logout.html", "AuthController (/auth/logout)", "Xoá cookie jwt."),
            ("Danh sách sản phẩm", "web/products.html", "ProductController (/products)", "Tìm kiếm, lọc danh mục, sắp xếp, phân trang."),
            ("Chi tiết sản phẩm", "web/product-detail.html", "ProductController (/products/{id})", "Ảnh, mô tả, specs, variants, review, sản phẩm liên quan."),
            ("Trang shop", "web/shop.html", "ShopController (/shop/{id})", "Thông tin shop, lọc giá/danh mục, sản phẩm nổi bật, đánh giá."),
            ("Giỏ hàng", "web/cart.html", "CartController (/cart)", "Sản phẩm nhóm theo shop, cập nhật số lượng, áp voucher."),
            ("Checkout", "web/checkout.html", "OrderController (/checkout)", "Form đặt hàng, áp voucher, chọn phương thức thanh toán."),
            ("Đặt hàng", "CartController (/api/cart/apply-voucher)", "POST /checkout/apply-voucher", "Trả về JSON discount."),
            ("Lịch sử đơn", "web/orders.html", "OrderController (/orders)", "Danh sách đơn, lọc trạng thái."),
            ("Chi tiết đơn", "web/order-detail.html", "OrderController (/orders/{id})", "Sản phẩm, trạng thái, tổng tiền."),
            ("Voucher công khai", "vouchers/list.html", "VoucherController (/vouchers)", "Voucher WEB khả dụng cho Customer."),
            ("My reviews", "web/my-reviews.html", "ReviewController (/my-reviews)", "Danh sách review của user hiện tại."),
            ("Trang pháp lý", "legal/*.html", "LegalDocumentController (/legal/{type})", "Hiển thị Terms, Privacy, Shipping..."),
            ("Trang lỗi", "error/400, 403, 404, 409, 500", "GlobalExceptionHandler", "Hiển thị thông báo lỗi tương ứng."),
        ],
        col_widths=[Cm(3.4), Cm(3.4), Cm(4.0), Cm(5.7)],
    )
    add_table_text_block(doc)

    add_heading(doc, "3.1.2. Giao diện Vendor", level=3)
    add_table_caption(doc, "Bảng 3.2. Giao diện phía Vendor")
    add_table(
        doc,
        header_row=["Giao diện", "Template", "Controller", "Chức năng chính"],
        data_rows=[
            ("Dashboard vendor", "vendor/dashboard.html", "VendorDashboardController (/vendor/dashboard)", "Doanh thu, sản phẩm nổi bật."),
            ("Quản lý shop", "vendor/shop-create.html, vendor/shop-update.html", "VendorShopController", "Tạo/cập nhật shop, logo, banner."),
            ("Quản lý sản phẩm", "vendor/products.html, vendor/product-form.html", "VendorProductController", "CRUD sản phẩm, upload ảnh."),
            ("Quản lý đơn hàng", "vendor/orders.html, vendor/order-detail.html", "VendorOrderController", "Xem, cập nhật trạng thái đơn."),
            ("Quản lý voucher SHOP", "vendor/voucher-list.html, voucher-create.html, voucher-edit.html", "VoucherController (/vendor/vouchers)", "CRUD voucher SHOP."),
            ("Hồ sơ vendor", "vendor/profile.html", "VendorProfileController", "Thông tin tài khoản, shop, KYC."),
            ("Form KYC", "vendor/kyc-form.html", "KycController (/vendor/kyc)", "Submit hồ sơ xác minh."),
        ],
        col_widths=[Cm(3.4), Cm(4.4), Cm(3.6), Cm(5.1)],
    )
    add_table_text_block(doc)

    add_heading(doc, "3.1.3. Giao diện Admin", level=3)
    add_table_caption(doc, "Bảng 3.3. Giao diện phía Admin")
    add_table(
        doc,
        header_row=["Giao diện", "Template", "Controller", "Chức năng chính"],
        data_rows=[
            ("Dashboard admin", "admin/dashboard.html", "AdminDashboardController", "Thống kê tổng quan, doanh thu."),
            ("Quản lý người dùng", "admin/user-list.html, user-detail.html", "AdminUserController", "Khóa/mở tài khoản, lọc role/status."),
            ("Quản lý shop", "admin/shop-list.html, shop-detail.html", "AdminShopController", "Approve, activate, deactivate, reject."),
            ("Quản lý danh mục", "admin/category-manage.html", "AdminCategoryController", "CRUD, lọc active."),
            ("Quản lý đơn hàng", "admin/order-list.html, order-detail.html", "AdminOrderController", "Danh sách và chi tiết (read-only)."),
            ("Quản lý sản phẩm", "admin/product-list.html, product-detail.html", "AdminProductController", "Hide/unhide, delete."),
            ("Quản lý review", "admin/review-list.html", "AdminReviewController", "Xem, lọc rating, xóa review."),
            ("Quản lý banner", "admin/banner-list.html, banner-form.html", "AdminBannerController", "CRUD banner, publish/unpublish."),
            ("Quản lý voucher WEB", "admin/voucher-list.html, voucher-create.html, voucher-edit.html", "VoucherController (/admin/vouchers)", "CRUD voucher WEB, activate/deactivate."),
            ("Duyệt KYC", "admin/kyc-list.html, kyc-detail.html", "AdminKycController", "Duyệt KYC vendor."),
        ],
        col_widths=[Cm(3.4), Cm(4.4), Cm(3.6), Cm(5.1)],
    )
    add_table_text_block(doc)

    add_heading(doc, "3.1.4. Giao diện Moderator", level=3)
    add_table_caption(doc, "Bảng 3.4. Giao diện phía Moderator")
    add_table(
        doc,
        header_row=["Giao diện", "Template", "Controller", "Chức năng chính"],
        data_rows=[
            ("Hàng chờ kiểm duyệt", "moderator/queue.html", "ModeratorController", "Danh sách ReportCase."),
            ("Chi tiết ReportCase", "moderator/case-detail.html", "ModeratorReportCaseController", "Approve, Reject, Escalate."),
            ("Hàng chờ review", "moderator/review-queue.html", "ModeratorReviewController", "Ẩn/khôi phục review."),
        ],
        col_widths=[Cm(3.4), Cm(4.4), Cm(3.6), Cm(5.1)],
    )
    add_table_text_block(doc)

    add_heading(doc, "3.1.5. Quan hệ Form/Page với Controller", level=3)
    add_paragraph(doc,
        "Mỗi giao diện Thymeleaf là view của một method Controller tương ứng. Khi người dùng submit form, "
        "request được gửi đến cùng URL hoặc URL do Controller chỉ định; ServiceImpl thực hiện logic nghiệp vụ "
        "và trả về đối tượng Model hoặc chuyển hướng (redirect). Kết quả được render lại bằng template tương ứng, "
        "kèm flash attribute cho thông báo thành công/thất bại.")
    add_paragraph(doc,
        "Một số form đặc biệt:")
    add_bullet(doc, "Áp voucher: POST /api/cart/apply-voucher trả về JSON { success, discount, finalTotal, voucher }, JS phía client cập nhật UI tại trang cart/checkout.", level=0)
    add_bullet(doc, "Review: POST /products/{productId}/reviews sau khi tạo sẽ redirect về /products/{id} kèm ?error nếu thất bại.", level=0)
    add_bullet(doc, "Upload ảnh: /api/upload được ProductController, BannerController và ShopController sử dụng để lưu file vào /uploads/. WebMvcConfig ánh xạ URL /uploads/** tới classpath/static/uploads.", level=0)
    add_bullet(doc, "KYC: /vendor/kyc gọi KycService xử lý submit, sau khi xong sẽ chuyển trạng thái sang PENDING_THIRD_PARTY và redirect về /vendor/profile.", level=0)


def write_3_2(doc):
    add_heading(doc, "3.2. Tầng nghiệp vụ", level=2)
    add_paragraph(doc,
        "Tầng nghiệp vụ được tổ chức theo nguyên tắc Interface-Implementation. Mỗi interface Service khai báo "
        "các phương thức nghiệp vụ, ServiceImpl tương ứng (package service.impl) cài đặt chi tiết. Các "
        "ServiceImpl sử dụng @RequiredArgsConstructor (Lombok) để inject các Repository và Service khác.")

    add_heading(doc, "3.2.1. Nhóm Authentication và User", level=3)
    add_bullet(doc, "AuthService / AuthServiceImpl: register(RegisterReq) xác thực email chưa tồn tại, mã hoá mật khẩu, lưu User; login(email, password) kiểm tra mật khẩu và trạng thái ACTIVE, ghi AuditLog LOGIN_SUCCESS/LOGIN_FAILED; cập nhật Terms/Privacy version.", level=0)
    add_bullet(doc, "VendorService / VendorServiceImpl: getShopIdFromUser, getShopByCurrentVendor, validateProductOwnership cho vendor hiện tại.", level=0)
    add_bullet(doc, "AuditLogService / AuditLogServiceImpl: logInfo, logWarning, logCritical; nhận AuditAction, resourceType, resourceId, actorId/Email/Role, reason; persist vào AuditLog.", level=0)

    add_heading(doc, "3.2.2. Nhóm Product", level=3)
    add_bullet(doc, "ProductService / ProductServiceImpl: createProduct, updateProduct, deleteProduct (ownership guard); cập nhật moderationStatus sang PENDING_MANUAL, ghi ModerationHistory và AuditEvent.", level=0)
    add_bullet(doc, "AutoModerationResultProvider: best-effort lấy kết quả Auto Moderation; nếu không có backend thật trả về Optional.empty.", level=0)
    add_bullet(doc, "AdminProductService / AdminProductServiceImpl: listProducts(search, status), getProductById (hỗ trợ cả String id lẫn ObjectId), hideProduct, unhideProduct (atomic update), deleteProduct.", level=0)

    add_heading(doc, "3.2.3. Nhóm Cart và Order", level=3)
    add_bullet(doc, "CartService / CartServiceImpl: getCartByUserId (lazy create), addToCart (kiểm tra tồn kho), updateCartItem, removeFromCart, setAppliedVoucherCode, getAppliedVoucherCode, countItems, calculateTotal, clearCart, removeItems (partial).", level=0)
    add_bullet(doc, "OrderService / OrderServiceImpl: createOrder (transactional) lấy Cart, lọc items, validate tồn kho, tính subtotal + shippingFee (15.000 VND) + discount từ VoucherService.validateForCheckout, lưu Order, cập nhật tồn kho, dọn giỏ; updateShippingStatus, hasUserPurchasedProduct, hasUserReceivedProduct, getOrderById.", level=0)
    add_bullet(doc, "VoucherApplicationGateway / Impl: reserveVoucher, releaseVoucher (atomic, sử dụng MongoTemplate updateFirst với $expr).", level=0)

    add_heading(doc, "3.2.4. Nhóm Voucher", level=3)
    add_bullet(doc, "VoucherService / VoucherServiceImpl: createVoucher (SHOP), createWebVoucher, updateVoucher/updateWebVoucher, activateWebVoucher, deactivateWebVoucher, deleteWebVoucher, validateForCheckout (3 bước: active, thời hạn, lượt dùng; kèm ràng buộc shop/sản phẩm), tryIncrementUsed, tryDecrementUsed, getWebVouchers (hỗ trợ cả type=WEB lẫn type=null).", level=0)

    add_heading(doc, "3.2.5. Nhóm Review và Moderation", level=3)
    add_bullet(doc, "ReviewService / ReviewServiceImpl: createReview (chỉ cho user đã DELIVERED), updateReview, deleteReview, hasUserReviewedProduct, canUserReviewProduct, getAverageRatingByProductId (cập nhật Product.rating), reportReview (tự chuyển REPORTED khi đạt AUTO_REPORT_THRESHOLD = 3 và tạo ReportCase), hideReview, restoreReview, deleteReviewByModerator.", level=0)
    add_bullet(doc, "ReportCaseService / Impl: createCase, listCases, decide (approve/reject/escalate); đồng bộ ModerationStatus trên Product/Review, ghi AuditLog.", level=0)
    add_bullet(doc, "EscalationService: tạo Escalation cho Admin xử lý; cập nhật ReportCase sang ESCALATED.", level=0)
    add_bullet(doc, "ViolationService: tạo Violation cho shop; đếm activeViolations theo shopId.", level=0)

    add_heading(doc, "3.2.6. Nhóm Shop, KYC và Banner", level=3)
    add_bullet(doc, "VendorService: tạo/cập nhật shop, upload logo/banner, kiểm tra quyền sở hữu shop.", level=0)
    add_bullet(doc, "AdminShopService: approveShop, activateShop, deactivateShop (lưu lý do + actorId), rejectShop; ghi AuditLog.", level=0)
    add_bullet(doc, "KycService: submitKyc, thirdPartyVerify, adminApprove, adminReject; cập nhật KycProfile.status và đồng bộ sang User.kycStatus, Shop.kycStatus.", level=0)
    add_bullet(doc, "AdminCategoryService: listCategories (search, active), createCategory, updateCategory, deleteCategory.", level=0)
    add_bullet(doc, "AdminBannerService: CRUD banner, publish/unpublish.", level=0)

    add_heading(doc, "3.2.7. Nhóm Complaint, Return, Refund", level=3)
    add_bullet(doc, "ComplaintService: tạo Complaint (LEVEL_0), vendorResponse (LEVEL_0), moderatorAssign (LEVEL_1), adminAssign (LEVEL_2), escalate, resolve; theo dõi deadline và escalationCount.", level=0)
    add_bullet(doc, "ReturnService: tạo ReturnRequest từ Complaint/Order; cập nhật status theo ReturnStatus.", level=0)
    add_bullet(doc, "RefundService: tạo RefundRequest (integration-ready) cho Complaint/Return/Order; chờ provider xử lý.", level=0)

    add_heading(doc, "3.2.8. Code đặc trưng", level=3)
    add_paragraph(doc,
        "Dưới đây là một số đoạn code tiêu biểu, được lấy trực tiếp từ source hiện tại nhằm minh hoạ nghiệp "
        "vụ chính của hệ thống.")

    add_paragraph(doc, "Đoạn 1. AuthServiceImpl.register - xác thực email, chặn role ADMIN/MODERATOR public, "
                       "BCrypt mật khẩu và ghi AuditLog.")
    add_code_in_table(doc,
        "package com.ecommerce.cnj70.service.impl;\n"
        "public User register(RegisterReq request) {\n"
        "    if (request.getAcceptTerms() == null || !request.getAcceptTerms()) {\n"
        "        throw new BadRequestException(\"Bạn phải đồng ý với Điều khoản sử dụng để đăng ký\");\n"
        "    }\n"
        "    if (existsByEmail(request.getEmail())) {\n"
        "        throw new BadRequestException(\"Email đã tồn tại\");\n"
        "    }\n"
        "    UserRole role = UserRole.CUSTOMER;\n"
        "    if (request.getRole() != null && !request.getRole().isBlank()) {\n"
        "        UserRole requested = UserRole.valueOf(request.getRole().toUpperCase());\n"
        "        if (requested == UserRole.ADMIN)\n"
        "            throw new BadRequestException(\"ADMIN role cannot be created via public registration\");\n"
        "        if (requested == UserRole.MODERATOR)\n"
        "            throw new BadRequestException(\"MODERATOR role cannot be created via public registration\");\n"
        "        role = requested;\n"
        "    }\n"
        "    User user = User.builder()\n"
        "            .email(request.getEmail())\n"
        "            .password(passwordEncoder.encode(request.getPassword()))\n"
        "            .fullName(request.getFullName())\n"
        "            .role(role)\n"
        "            .status(AccountStatus.ACTIVE)\n"
        "            .acceptedTermsAt(LocalDateTime.now())\n"
        "            .acceptedTermsVersion(getCurrentVersion(LegalDocumentType.TERMS))\n"
        "            .build();\n"
        "    return userRepository.save(user);\n"
        "}"
    )

    add_paragraph(doc, "Đoạn 2. OrderServiceImpl.createOrder - transaction tạo Order, trừ tồn kho, áp voucher và dọn giỏ.")
    add_code_in_table(doc,
        "package com.ecommerce.cnj70.service.impl;\n"
        "private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(15000);\n"
        "@Transactional(rollbackFor = Exception.class)\n"
        "public Order createOrder(String userId, CheckoutReq request) {\n"
        "    Cart cart = cartRepository.findByUserId(userId)\n"
        "            .orElseThrow(() -> new BadRequestException(\"Không tìm thấy giỏ hàng\"));\n"
        "    BigDecimal subtotal = BigDecimal.ZERO;\n"
        "    for (Cart.CartItem cartItem : cart.getItems()) {\n"
        "        Product product = productRepository.findById(cartItem.getProductId())\n"
        "                .orElseThrow(() -> new ResourceNotFoundException(\"Không tìm thấy sản phẩm\"));\n"
        "        if (product.getStock() < cartItem.getQuantity()) {\n"
        "            throw new BadRequestException(\"Sản phẩm không đủ hàng\");\n"
        "        }\n"
        "        product.setStock(product.getStock() - cartItem.getQuantity());\n"
        "        productRepository.save(product);\n"
        "        subtotal = subtotal.add(cartItem.getPrice()\n"
        "                .multiply(BigDecimal.valueOf(cartItem.getQuantity())));\n"
        "    }\n"
        "    BigDecimal totalAmount = subtotal.add(SHIPPING_FEE);\n"
        "    Order order = Order.builder()\n"
        "            .userId(userId)\n"
        "            .items(buildOrderItems(cart))\n"
        "            .subtotal(subtotal)\n"
        "            .shippingFee(SHIPPING_FEE)\n"
        "            .totalAmount(totalAmount)\n"
        "            .status(OrderStatus.PENDING)\n"
        "            .paymentMethod(PaymentMethod.COD)\n"
        "            .build();\n"
        "    return orderRepository.save(order);\n"
        "}"
    )

    add_paragraph(doc, "Đoạn 3. VoucherServiceImpl.validateForCheckout - kiểm tra 3 bước active/thời hạn/lượt dùng và ràng buộc shop/sản phẩm.")
    add_code_in_table(doc,
        "package com.ecommerce.cnj70.service.impl;\n"
        "public Voucher validateForCheckout(String code, String shopId, String productId) {\n"
        "    Voucher voucher = getVoucherByCode(code);\n"
        "    if (!voucher.isActive())\n"
        "        throw new BadRequestException(\"Voucher đã bị vô hiệu hóa\");\n"
        "    LocalDateTime now = LocalDateTime.now();\n"
        "    if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate()))\n"
        "        throw new BadRequestException(\"Voucher chưa bắt đầu\");\n"
        "    if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate()))\n"
        "        throw new BadRequestException(\"Voucher đã hết hạn\");\n"
        "    if (voucher.getUsed() >= voucher.getQuantity())\n"
        "        throw new BadRequestException(\"Voucher đã hết lượt sử dụng\");\n"
        "    if (voucher.getType() == VoucherType.SHOP && shopId != null\n"
        "            && !voucher.getShopId().equals(shopId))\n"
        "        throw new BadRequestException(\"Voucher không áp dụng cho shop này\");\n"
        "    return voucher;\n"
        "}"
    )

    add_paragraph(doc, "Đoạn 4. ReviewServiceImpl.reportReview - tăng reportCount, tự chuyển REPORTED khi đạt ngưỡng và tạo ReportCase.")
    add_code_in_table(doc,
        "package com.ecommerce.cnj70.service.impl;\n"
        "private static final int AUTO_REPORT_THRESHOLD = 3;\n"
        "public void reportReview(String reviewId, String reporterId, String reason) {\n"
        "    Review review = reviewRepository.findById(reviewId)\n"
        "            .orElseThrow(() -> new ResourceNotFoundException(\"Không tìm thấy đánh giá\"));\n"
        "    review.setReportCount(review.getReportCount() + 1);\n"
        "    if (review.getReportCount() >= AUTO_REPORT_THRESHOLD) {\n"
        "        review.setModerationStatus(ReviewModerationStatus.REPORTED);\n"
        "        review.setModerationReason(\"Tự động chuyển sang REPORTED: \" + review.getReportCount());\n"
        "        review.setModeratedAt(LocalDateTime.now());\n"
        "        auditLogService.logWarning(AuditAction.REVIEW_REPORTED, ...);\n"
        "    }\n"
        "    reviewRepository.save(review);\n"
        "    reportCaseService.createCase(ReportCaseCreateReq.builder()\n"
        "            .targetType(ReportTargetType.REVIEW)\n"
        "            .targetId(reviewId)\n"
        "            .reason(reason).build(), reporterId, reporterUsername);\n"
        "}"
    )

    add_paragraph(doc, "Đoạn 5. AdminProductServiceImpl.hideProduct - atomic update trực tiếp trên collection tránh lỗi E11000 duplicate key.")
    add_code_in_table(doc,
        "package com.ecommerce.cnj70.service.impl;\n"
        "@Transactional\n"
        "public Product hideProduct(String id) {\n"
        "    Product product = getProductById(id);\n"
        "    if (product.getStatus() == ProductStatus.HIDDEN) {\n"
        "        throw new BadRequestException(\"Sản phẩm đã ở trạng thái Ẩn\");\n"
        "    }\n"
        "    Object nativeId = resolveIdForQuery(id, product);\n"
        "    org.bson.Document update = new org.bson.Document(\"$set\",\n"
        "            new org.bson.Document(\"status\", ProductStatus.HIDDEN.name()));\n"
        "    long matched = mongoTemplate.getCollection(\"products\")\n"
        "            .updateOne(new org.bson.Document(\"_id\", nativeId), update)\n"
        "            .getMatchedCount();\n"
        "    return getProductById(id);\n"
        "}"
    )

    add_paragraph(doc, "Đoạn 6. SecurityConfig - SecurityFilterChain thiết lập STATELESS, JWT filter và phân quyền URL.")
    add_code_in_table(doc,
        "package com.ecommerce.cnj70.config;\n"
        "@Bean\n"
        "public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {\n"
        "    http\n"
        "        .csrf(csrf -> csrf.disable())\n"
        "        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))\n"
        "        .authorizeHttpRequests(auth -> auth\n"
        "            .requestMatchers(\"/\", \"/home\", \"/auth/**\", \"/products/**\",\n"
        "                \"/vouchers\", \"/cart/count\", \"/css/**\", \"/js/**\",\n"
        "                \"/images/**\", \"/uploads/**\", \"/webjars/**\").permitAll()\n"
        "            .requestMatchers(\"/vendor/**\").hasRole(\"VENDOR\")\n"
        "            .requestMatchers(\"/admin/**\").hasAnyRole(\"ADMIN\", \"MODERATOR\")\n"
        "            .requestMatchers(\"/moderator/**\").hasRole(\"MODERATOR\")\n"
        "            .anyRequest().authenticated())\n"
        "        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);\n"
        "    return http.build();\n"
        "}"
    )


def write_3_3(doc):
    add_heading(doc, "3.3. Tầng thực thể", level=2)
    add_paragraph(doc,
        "Tầng thực thể được tổ chức trong package document. Mỗi class được đánh dấu @Document(collection = \"...\") "
        "tương ứng với một collection MongoDB. Các trường thường truy vấn được chỉ mục bằng @Indexed; @CreatedDate "
        "và @LastModifiedDate được tự động quản lý nhờ @EnableMongoAuditing trong MongoConfig.")

    add_heading(doc, "3.3.1. Tổng hợp Document và Collection", level=3)
    add_table_caption(doc, "Bảng 3.5. Tổng hợp 24 Document của hệ thống")
    add_table(
        doc,
        header_row=["STT", "Document", "Collection", "Vai trò"],
        data_rows=[
            ("1", "User", "users", "Tài khoản người dùng."),
            ("2", "Shop", "shops", "Cửa hàng vendor."),
            ("3", "Product", "products", "Sản phẩm."),
            ("4", "ProductSpecification", "Nhúng trong Product", "Thông số kỹ thuật."),
            ("5", "ProductVariant", "Nhúng trong Product", "Biến thể sản phẩm."),
            ("6", "Category", "categories", "Danh mục sản phẩm."),
            ("7", "Order", "orders", "Đơn hàng."),
            ("8", "Order.OrderItem (inner)", "Nhúng trong Order", "Dòng sản phẩm trong đơn."),
            ("9", "Order.SubOrderShipping (inner)", "Nhúng trong Order", "Vận chuyển theo shop."),
            ("10", "Cart", "carts", "Giỏ hàng."),
            ("11", "Cart.CartItem (inner)", "Nhúng trong Cart", "Dòng sản phẩm trong giỏ."),
            ("12", "Voucher", "vouchers", "Mã giảm giá."),
            ("13", "Review", "reviews", "Đánh giá sản phẩm."),
            ("14", "Banner", "banners", "Banner quảng cáo."),
            ("15", "KycProfile", "kyc_profiles", "Hồ sơ KYC vendor."),
            ("16", "LegalDocument", "legal_documents", "Tài liệu pháp lý."),
            ("17", "AuditLog", "audit_logs", "Nhật ký sự kiện (chi tiết)."),
            ("18", "AuditLogEntry", "audit_logs", "Nhật ký truy vấn (append-only)."),
            ("19", "ModerationHistory", "moderation_history", "Lịch sử kiểm duyệt sản phẩm."),
            ("20", "ReportCase", "report_cases", "Phiên kiểm duyệt."),
            ("21", "Escalation", "escalations", "Phiếu leo thang cho Admin."),
            ("22", "Complaint", "complaints", "Khiếu nại 3 cấp."),
            ("23", "ReturnRequest", "returns", "Yêu cầu hoàn trả."),
            ("24", "RefundRequest", "refunds", "Yêu cầu hoàn tiền (integration-ready)."),
            ("25", "Violation", "violations", "Vi phạm shop."),
            ("26", "SchedulerIdempotencyRecord", "scheduler_idempotency", "Idempotency cho scheduler."),
            ("27", "FlashSaleStat", "Không persist", "View-model cho trang chủ."),
        ],
        col_widths=[Cm(1.2), Cm(5.0), Cm(4.0), Cm(6.3)],
    )
    add_table_text_block(doc)

    add_heading(doc, "3.3.2. Quan hệ giữa các Document", level=3)
    add_paragraph(doc,
        "Các quan hệ chính giữa các Document trong hệ thống (dựa trên các trường id/reference có trong source):")
    add_bullet(doc, "User.shopId → Shop._id: Mỗi Vendor có một ShopId trỏ sang Shop tương ứng (xem User.shopId).", level=0)
    add_bullet(doc, "Product.shopId → Shop._id: Mỗi Product thuộc về một Shop.", level=0)
    add_bullet(doc, "Product.categoryId → Category._id: Mỗi Product thuộc một Category; Category.parentId hỗ trợ danh mục cha-con.", level=0)
    add_bullet(doc, "Cart.userId → User._id: Mỗi Cart thuộc một User, duy nhất theo userId.", level=0)
    add_bullet(doc, "CartItem.productId → Product._id, CartItem.shopId → Shop._id.", level=0)
    add_bullet(doc, "Order.userId → User._id; Order.items[].shopId/productId tham chiếu Shop và Product.", level=0)
    add_bullet(doc, "Order.shippingByShop (key = shopId) → SubOrderShipping: Vận chuyển theo từng shop trong đơn.", level=0)
    add_bullet(doc, "Voucher.shopId → Shop._id (nếu type=SHOP); Voucher.productIds → Product._id (nếu type=WEB).", level=0)
    add_bullet(doc, "Review.productId, Review.userId → Product._id, User._id.", level=0)
    add_bullet(doc, "KycProfile.userId → User._id (unique).", level=0)
    add_bullet(doc, "ReportCase.targetId/referentId trỏ về Product/Review; ReportCase.reporterId, assignedModeratorId trỏ về User.", level=0)
    add_bullet(doc, "Escalation.reportCaseId → ReportCase._id; Escalation.assignedAdminId, decisionAdminId trỏ về User.", level=0)
    add_bullet(doc, "Complaint.customerId/shopId/vendorId/orderId trỏ về User, Shop, Order; Complaint.assignedModeratorId/assignedAdminId trỏ về User.", level=0)
    add_bullet(doc, "ReturnRequest.complaintId, orderId, customerId, shopId, vendorId trỏ về các Document tương ứng; ReturnRequest.refundId → RefundRequest._id.", level=0)
    add_bullet(doc, "RefundRequest.complaintId, returnId, orderId, customerId, shopId, vendorId trỏ về các Document tương ứng.", level=0)
    add_bullet(doc, "Violation.shopId, productId, orderId trỏ về Shop, Product, Order.", level=0)
    add_bullet(doc, "AuditLog/AuditLogEntry.resourceId tham chiếu các Document khác (User, Shop, Product, Review, Order...).", level=0)

    add_heading(doc, "3.3.3. Thuộc tính chính của từng Document", level=3)
    add_paragraph(doc,
        "Bảng dưới tổng hợp các thuộc tính chính của các Document quan trọng nhất dựa trên class trong source.")

    add_table_caption(doc, "Bảng 3.6. Thuộc tính chính của User")
    add_table(
        doc,
        header_row=["Thuộc tính", "Kiểu", "Ghi chú"],
        data_rows=[
            ("id", "String", "Primary key, @Id."),
            ("email", "String", "@Indexed(unique = true)."),
            ("password", "String", "BCrypt hash."),
            ("fullName, phone, address", "String", "Thông tin cá nhân."),
            ("role", "UserRole", "ADMIN/VENDOR/CUSTOMER/MODERATOR."),
            ("status", "AccountStatus", "UNVERIFIED/ACTIVE/BANNED/LOCKED... Mặc định UNVERIFIED."),
            ("shopId", "String", "Reference sang Shop (nếu là vendor)."),
            ("avatarUrl", "String", "Ảnh đại diện."),
            ("encryptedCitizenId / encryptedTaxCode / encryptedBankAccount", "String", "AES-256-GCM."),
            ("acceptedTermsAt/Version, acceptedPrivacyAt/Version", "LocalDateTime/Integer", "Theo dõi Terms/Privacy."),
            ("marketingOptIn", "Boolean", "Opt-in marketing."),
            ("kycStatus", "KycStatus", "Đồng bộ từ KycProfile."),
            ("enforcementActorId/Reason/At", "String", "Admin enforcement."),
            ("createdAt, updatedAt", "LocalDateTime", "Mongo auditing."),
        ],
        col_widths=[Cm(6.0), Cm(3.5), Cm(7.0)],
    )
    add_table_text_block(doc)

    add_table_caption(doc, "Bảng 3.7. Thuộc tính chính của Shop")
    add_table(
        doc,
        header_row=["Thuộc tính", "Kiểu", "Ghi chú"],
        data_rows=[
            ("id", "String", "@Id."),
            ("ownerId", "String", "User.id của chủ shop."),
            ("shopName", "String", "@Indexed(unique = true)."),
            ("description, logoUrl, bannerUrl", "String", "Thông tin hiển thị."),
            ("status", "ShopStatus", "PENDING/APPROVED/REJECTED/SUSPENDED."),
            ("active", "boolean", "Cờ ngừng hoạt động."),
            ("kycStatus, kycReferenceId, kycApprovedAt, kycRejectionReason, kycSubmittedAt, kycDocumentVersion", "Tuỳ enum/LocalDateTime", "Đồng bộ từ KycProfile."),
            ("encryptedCitizenId, encryptedTaxCode, encryptedBankAccount", "String", "AES-256-GCM (backup)."),
            ("rejectionReason, deactivationReason, actionBy, actionAt", "String/LocalDateTime", "Admin shop lifecycle."),
            ("enforcementActorId/Reason/At", "String", "Phase 3A enforcement."),
            ("createdAt, updatedAt", "LocalDateTime", "Mongo auditing."),
        ],
        col_widths=[Cm(6.0), Cm(3.5), Cm(7.0)],
    )
    add_table_text_block(doc)

    add_table_caption(doc, "Bảng 3.8. Thuộc tính chính của Product")
    add_table(
        doc,
        header_row=["Thuộc tính", "Kiểu", "Ghi chú"],
        data_rows=[
            ("id", "String", "@Id."),
            ("shopId, shopName", "String", "Reference Shop."),
            ("name", "String", "@Indexed."),
            ("brand, warrantyMonths, manufacturer, manufacturerAddress", "tuỳ kiểu", "Thông tin thương hiệu."),
            ("description, richDescription", "String", "Mô tả ngắn và mô tả chi tiết."),
            ("price", "BigDecimal", "Giá bán."),
            ("stock", "int", "Tồn kho."),
            ("categoryId, categoryName", "String", "Reference Category."),
            ("imageUrls, thumbnailUrl", "List<String>/String", "Ảnh sản phẩm."),
            ("specifications", "List<ProductSpecification>", "Thông số kỹ thuật."),
            ("variants", "List<ProductVariant>", "Biến thể."),
            ("status", "ProductStatus", "DRAFT/ACTIVE/HIDDEN/OUT_OF_STOCK/ARCHIVED."),
            ("moderationStatus, moderationActorId, moderationReason, moderationAt", "tuỳ enum/String/LocalDateTime", "Hàng chờ Moderator."),
            ("enforcementActorId/Reason/At", "String", "Admin enforcement."),
            ("rating, reviewCount, sold", "double/int/int", "Thống kê."),
            ("createdAt, updatedAt", "LocalDateTime", "Mongo auditing."),
        ],
        col_widths=[Cm(6.0), Cm(3.5), Cm(7.0)],
    )
    add_table_text_block(doc)

    add_table_caption(doc, "Bảng 3.9. Thuộc tính chính của Order")
    add_table(
        doc,
        header_row=["Thuộc tính", "Kiểu", "Ghi chú"],
        data_rows=[
            ("id", "String", "@Id."),
            ("userId, userName, userEmail, userPhone", "String", "Thông tin khách hàng."),
            ("shippingAddress", "String", "Địa chỉ giao hàng."),
            ("items", "List<OrderItem>", "Chi tiết sản phẩm (shopId, shopName, productId, name, image, price, qty, subtotal)."),
            ("subtotal, shippingFee, totalAmount, discount", "BigDecimal", "Tổng tiền và giảm giá."),
            ("voucherId, voucherCode, voucherName", "String", "Voucher snapshot."),
            ("status", "OrderStatus", "PENDING/PREPARING/SHIPPING/DELIVERED/CANCELLED."),
            ("paymentMethod", "PaymentMethod", "COD/BANK_TRANSFER/MOMO/VNPAY."),
            ("paid", "boolean", "Trạng thái thanh toán."),
            ("shopId, shopName", "String", "Shop chính (giữ tương thích cũ)."),
            ("shippingByShop", "Map<String, SubOrderShipping>", "Vận chuyển theo shop (key = shopId)."),
            ("createdAt, updatedAt, deliveredAt", "LocalDateTime", "Mongo auditing + thời điểm giao."),
        ],
        col_widths=[Cm(6.0), Cm(3.5), Cm(7.0)],
    )
    add_table_text_block(doc)

    add_table_caption(doc, "Bảng 3.10. Thuộc tính chính của Voucher, Review, Banner và KycProfile")
    add_table(
        doc,
        header_row=["Document", "Thuộc tính chính"],
        data_rows=[
            ("Voucher", "id, code(@Indexed unique), name, type(VoucherType SHOP/WEB), shopId, shopName, productIds, discountType(DiscountType PERCENT/AMOUNT), discountValue, maxDiscountAmount, minOrderValue, quantity, used, startDate, endDate, active, createdBy, createdAt, updatedAt."),
            ("Review", "id, productId(@Indexed), userId(@Indexed), userName, userAvatar, rating(1-5), comment, moderationStatus(ReviewModerationStatus), moderationReason, moderatedBy, moderatedAt, reportCount, moderationActorId, moderationAt, hidden, hiddenReason, pipelineModerationStatus, createdAt, updatedAt."),
            ("Banner", "id, title, description, tag, tagIcon, imageUrl, link, ctaText, ctaIcon, theme, status(PUBLISHED/UNPUBLISHED), position, sortOrder, createdAt, updatedAt."),
            ("KycProfile", "id, userId(@Indexed unique), status(KycStatus), ownerFullName, idNumber, idFrontImageUrl, idBackImageUrl, businessName, taxCode, businessLicenseUrl, bankAccount, bankName, bankBranch, thirdPartyResult, thirdPartyRejectionReason, thirdPartyReferenceId, thirdPartyVerifiedAt, adminReviewerId/Name/Note/ReviewedAt, submittedAt, lastResubmittedAt, submitCount."),
        ],
        col_widths=[Cm(3.5), Cm(13.0)],
    )
    add_table_text_block(doc)

    add_table_caption(doc, "Bảng 3.11. Thuộc tính chính của ReportCase, Escalation, Complaint và Violation")
    add_table(
        doc,
        header_row=["Document", "Thuộc tính chính"],
        data_rows=[
            ("ReportCase", "id, resourceType(ReportCaseResourceType), resourceId, resourceName, targetType(ReportTargetType), targetId, targetSnapshot, reporterId, reporterName, reporterEmail, autoModerationStatus, assignedModeratorEmail/Id, source(USER/AUTO), reason, reportReason(ReportReason), description, evidence(List<String>), autoFlags(List<String>), status(ReportCaseStatus), decision(ReportCaseDecision), decisionReason, note, targetStatusBefore/After, priority, assignedAt, resolvedAt, escalationSeverity, escalationReason, escalationReviewed, decisionModeratorId/Email, decisionNote, decidedAt."),
            ("Escalation", "id, reportCaseId(@Indexed), resourceType(ReportCaseResourceType), resourceId, vendorId, shopId, reason, severity(EscalationSeverity), moderatorId, moderatorEmail, moderatorRole, sourceStatus(ReportCaseStatus), status(Status PENDING/IN_REVIEW/RESOLVED), assignedAdminId/Email, decisionAdminId/Email, decisionAction(ViolationAction), decisionNote, resolvedAt."),
            ("Complaint", "id, customerId, customerName, customerEmail, shopId, shopName, vendorId, orderId, orderItemIds(List<String>), reason(ComplaintReason), description, evidence(List<String>), status(ComplaintStatus), level(ComplaintLevel), vendorResponse, vendorRespondedAt/ById/ByEmail, assignedModeratorId/Email, moderatorAssignedAt, assignedAdminId/Email, adminAssignedAt, decision, decisionReason, resolvedAt, resolvedById/Email/Role, vendorResponseDeadline, moderatorResolutionDeadline, escalationCount."),
            ("Violation", "id, shopId, productId, productName, orderId, type(ViolationType), severity(ViolationSeverity), reason, adminNote, createdBy, resolvedAt, resolvedBy, resolutionNote."),
        ],
        col_widths=[Cm(3.5), Cm(13.0)],
    )
    add_table_text_block(doc)

    add_table_caption(doc, "Bảng 3.12. Thuộc tính chính của ReturnRequest, RefundRequest, AuditLog và AuditLogEntry")
    add_table(
        doc,
        header_row=["Document", "Thuộc tính chính"],
        data_rows=[
            ("ReturnRequest", "id, complaintId(@Indexed), orderId(@Indexed), orderItemIds(List<String>), customerId, customerName, shopId, shopName, vendorId, reason, evidence(List<String>), status(ReturnStatus), refundId, declaredAmount, createdAt, updatedAt."),
            ("RefundRequest", "id, complaintId(@Indexed), returnId(@Indexed), orderId(@Indexed), customerId, shopId, vendorId, declaredAmount, settledAmount, provider, providerTransactionId, status(RefundStatus), providerMessage, requestedById/Email/Role, settledAt, createdAt, updatedAt."),
            ("AuditLog", "id, actorId(@Indexed), actorUsername, actorRole, action(AuditAction, @Indexed), resourceType(@Indexed), resourceId(@Indexed), beforeState, afterState, ipAddress, userAgent, reason, severity(AuditSeverity), metadata(Map), createdAt(@Indexed)."),
            ("AuditLogEntry", "id, actorId, actorEmail, role(UserRole), action(@Indexed), resourceType(@Indexed), resourceId, reason, severity, before, after, ip, createdAt. Append-only."),
        ],
        col_widths=[Cm(3.5), Cm(13.0)],
    )
    add_table_text_block(doc)

    add_table_caption(doc, "Bảng 3.13. Thuộc tính chính của các Document còn lại")
    add_table(
        doc,
        header_row=["Document", "Thuộc tính chính"],
        data_rows=[
            ("Category", "id, name(@Indexed unique), description, iconUrl, parentId, sortOrder, active, createdAt, updatedAt."),
            ("Cart", "id, userId(@Indexed unique), items(List<CartItem>), appliedVoucherCode, updatedAt."),
            ("CartItem (inner)", "productId, productName, imageUrl, price, quantity, subtotal, shopId, shopName, stock."),
            ("OrderItem (inner)", "shopId, shopName, productId, productName, imageUrl, price, quantity, subtotal."),
            ("SubOrderShipping (inner)", "shopId, shopName, status(ShippingStatus), trackingNumber, carrier, shippedAt, deliveredAt, failureReason, note, updatedAt."),
            ("ProductSpecification", "name, value, unit."),
            ("ProductVariant", "specifications, price, stock, sku."),
            ("FlashSaleStat", "product, hasDiscount, discountPercent, soldCount, progressPercent (view-model, không persist)."),
            ("LegalDocument", "id, type(LegalDocumentType @Indexed unique), title, content, version, effectiveDate, updatedBy, createdAt, updatedAt, renderedContent, metaDescription."),
            ("ModerationHistory", "id, resourceType, resourceId(@Indexed), resourceName, action(ModerationAction), beforeStatus, afterStatus, reason, moderatorId, moderatorEmail, moderatorRole, createdAt(@Indexed)."),
            ("SchedulerIdempotencyRecord", "id, operationKey(unique), jobName, entityType, entityId, operation, status(SUCCESS/FAILED/SKIPPED), detail, createdAt(@Indexed), expiresAt."),
        ],
        col_widths=[Cm(4.5), Cm(12.0)],
    )
    add_table_text_block(doc)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main(out_path):
    doc = Document()
    setup_document(doc)

    write_2_1(doc)
    write_2_2(doc)
    write_2_6(doc)
    write_3_1(doc)
    write_3_2(doc)
    write_3_3(doc)

    doc.save(out_path)
    print(f"Saved: {out_path}")


if __name__ == "__main__":
    # output path
    if len(sys.argv) >= 2:
        out_path = sys.argv[1]
    else:
        out_path = os.path.join(
            "d:/Code Full/Java/BTL_E_commerce/Ecommerce_Java/docs",
            "BTL_CNJava_N05_Hoan_2.1_2.2_2.6_3.1_3.2_3.3.docx",
        )
    main(out_path)
