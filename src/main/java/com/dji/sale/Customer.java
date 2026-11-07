package com.dji.sale;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 客户值对象（不可变）。
 *
 * <p><b>零宽字符规范化（DECISIONS.md ADR-003，本次实施采用的默认方案）：</b>
 * 真实 order.json 中订单 2025102900009 客户名为 {@code \u200cElizabeth Taylor}
 * （首字符 U+200C）、2025102900010 为 {@code Emil\u200c}（尾字符 U+200C）。
 * 本对象保留 {@link #getRawName() rawName}（JSON 原文，供审计），
 * 同时提供剥离 Unicode 格式控制字符（{@code \p{Cf}}，含 U+200C/U+FEFF 等）
 * 并 trim 后的 {@link #getNormalizedName() normalizedName}，用于界面展示、
 * 关键字查询与按客户分组。</p>
 *
 * <p>不变量：normalizedName、email、phone 剥离后均非空白。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public final class Customer {

	/** Unicode 格式控制字符（ Cf 分类：零宽字符、软连字符、BOM 等）。 */
	private static final Pattern FORMAT_CHARS = Pattern.compile("\\p{Cf}+");

	private final String rawName;
	private final String normalizedName;
	private final String email;
	private final String phone;

	/**
	 * @param rawName JSON 原始客户名（保留审计）
	 * @param email   客户邮箱（非空白）
	 * @param phone   客户电话（非空白）
	 * @throws OrderValidationException 客户名/邮箱/电话为 null 或规范化后为空白
	 */
	public Customer(String rawName, String email, String phone) {
		if (rawName == null) {
			throw new OrderValidationException("customerName", "客户名不能为空");
		}
		this.rawName = rawName;
		this.normalizedName = FORMAT_CHARS.matcher(rawName).replaceAll("").trim();
		if (normalizedName.isEmpty()) {
			throw new OrderValidationException("customerName",
					"客户名去除不可见字符后为空: \"" + rawName + "\"");
		}
		this.email = requireNormalizedText(email, "email");
		this.phone = requireNormalizedText(phone, "phone");
	}

	private static String requireNormalizedText(String value, String fieldName) {
		if (value == null || FORMAT_CHARS.matcher(value).replaceAll("").trim().isEmpty()) {
			throw new OrderValidationException(fieldName, "客户" + fieldName + "不能为空");
		}
		return value.trim();
	}

	/** @return JSON 原始客户名（可能含零宽字符，仅供审计）。 */
	public String getRawName() {
		return rawName;
	}

	/** @return 规范化客户名（剥离零宽字符并 trim），用于展示/查询/分组。 */
	public String getNormalizedName() {
		return normalizedName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	/** 相等性基于规范化后的业务字段，保证含/不含零宽字符的同名客户视为同一客户。 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Customer customer)) {
			return false;
		}
		return normalizedName.equals(customer.normalizedName)
				&& email.equals(customer.email)
				&& phone.equals(customer.phone);
	}

	@Override
	public int hashCode() {
		return Objects.hash(normalizedName, email, phone);
	}

	@Override
	public String toString() {
		return normalizedName;
	}
}
