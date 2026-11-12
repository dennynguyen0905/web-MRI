package com.vrris.pacs.rule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentTreeExpanderTest {

    private static final DepartmentTreeExpander.Node KHOA = new DepartmentTreeExpander.Node(1L, null);
    private static final DepartmentTreeExpander.Node NHOM_A = new DepartmentTreeExpander.Node(2L, 1L);
    private static final DepartmentTreeExpander.Node NHOM_B = new DepartmentTreeExpander.Node(3L, 1L);
    private static final DepartmentTreeExpander.Node MAY = new DepartmentTreeExpander.Node(4L, 2L);
    private static final List<DepartmentTreeExpander.Node> TREE = List.of(KHOA, NHOM_A, NHOM_B, MAY);

    @Test
    void chon_khoa_goc_thi_lay_ca_cay_con_nhieu_cap() {
        assertThat(DepartmentTreeExpander.expand(TREE, List.of(1L))).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    @Test
    void chon_mot_nhom_thi_chi_lay_nhanh_con_cua_nhom_do() {
        assertThat(DepartmentTreeExpander.expand(TREE, List.of(2L))).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    void chon_la_khong_co_con_thi_tra_dung_chinh_no() {
        assertThat(DepartmentTreeExpander.expand(TREE, List.of(4L))).containsExactly(4L);
    }

    @Test
    void khong_chon_gi_thi_tra_rong() {
        assertThat(DepartmentTreeExpander.expand(TREE, List.of())).isEmpty();
    }

    @Test
    void id_khong_ton_tai_trong_cay_van_duoc_giu_nguyen() {
        Set<Long> result = DepartmentTreeExpander.expand(TREE, List.of(999L));
        assertThat(result).containsExactly(999L);
    }
}
