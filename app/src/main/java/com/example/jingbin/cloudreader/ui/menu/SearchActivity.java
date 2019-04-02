package com.example.jingbin.cloudreader.ui.menu;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.example.jingbin.cloudreader.R;
import com.example.jingbin.cloudreader.adapter.CategoryArticleAdapter;
import com.example.jingbin.cloudreader.adapter.GankAndroidSearchAdapter;
import com.example.jingbin.cloudreader.base.refreshadapter.JQuickAdapter;
import com.example.jingbin.cloudreader.bean.wanandroid.HomeListBean;
import com.example.jingbin.cloudreader.bean.wanandroid.SearchTagBean;
import com.example.jingbin.cloudreader.databinding.ActivitySearchBinding;
import com.example.jingbin.cloudreader.utils.BaseTools;
import com.example.jingbin.cloudreader.utils.CommonUtils;
import com.example.jingbin.cloudreader.utils.DebugUtil;
import com.example.jingbin.cloudreader.utils.ToastUtil;
import com.example.jingbin.cloudreader.view.MyDividerItemDecoration;
import com.example.jingbin.cloudreader.view.statusbar.StatusBarUtil;
import com.example.jingbin.cloudreader.view.webview.WebViewActivity;
import com.example.jingbin.cloudreader.viewmodel.wan.SearchViewModel;
import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import java.util.List;
import java.util.Objects;

/**
 * 搜索页面
 *
 * @author jingbin
 */
public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private JQuickAdapter mAdapter;
    private SearchViewModel viewModel;
    private String keyWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search);
        StatusBarUtil.setColor(this, CommonUtils.getColor(R.color.colorTheme), 0);
        viewModel = ViewModelProviders.of(this).get(SearchViewModel.class);
        binding.setViewModel(viewModel);
        initRefreshView();
        initView();
        initData();
    }

    private void initData() {
        viewModel.getHotkey().observe(this, new Observer<List<SearchTagBean.DataBean>>() {
            @Override
            public void onChanged(@Nullable List<SearchTagBean.DataBean> dataBeans) {
                if (dataBeans != null && dataBeans.size() > 0) {
                    showHotTagView(true);
                    showTagView(binding.tflSearch, dataBeans);
                } else {
                    showHotTagView(false);
                }
            }
        });
    }

    protected void initView() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String content = binding.etContent.getText().toString().trim();
                if (!TextUtils.isEmpty(content)) {
                    binding.ivClear.setVisibility(View.VISIBLE);
                    keyWord = content;
                    load();
                } else {
                    binding.ivClear.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etContent.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (!TextUtils.isEmpty(keyWord) && binding.tlSearch.getSelectedTabPosition() == 3 && actionId == EditorInfo.IME_ACTION_SEARCH) {
                /**
                 * www.baidu.com
                 * http://www.baidu.com
                 * youtube.com等
                 */
                if (Patterns.WEB_URL.matcher(keyWord).matches() || URLUtil.isValidUrl(keyWord)) {
                    if (!TextUtils.isEmpty(keyWord)) {
                        if (!keyWord.startsWith("http")) {
                            if (keyWord.startsWith("www")) {
                                keyWord = "http://" + keyWord;
                            } else {
                                keyWord = "http://www." + keyWord;
                            }
                        }
                        BaseTools.hideSoftKeyBoard(this);
                        WebViewActivity.loadUrl(this, keyWord, "加载中...");
                    }
                } else {
                    ToastUtil.showToast("请输入正确的链接~");
                }
            } else {
                BaseTools.hideSoftKeyBoard(this);
            }
            return false;
        });
        binding.ivClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyWord = "";
                binding.etContent.setText("");
                binding.etContent.setFocusable(true);
                binding.etContent.setFocusableInTouchMode(true);
                binding.etContent.requestFocus();
                showHotTagView(true);
                BaseTools.showSoftKeyBoard(SearchActivity.this, binding.etContent);
            }
        });


        binding.tlSearch.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                initViewModel(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        initViewModel(0);
    }

    private void load() {
        int tabPosition = binding.tlSearch.getSelectedTabPosition();
        DebugUtil.error("tabPosition:" + tabPosition);
        switch (tabPosition) {
            case 0:
                viewModel.setPage(0);
                loadWanData();
                break;
            case 1:
                viewModel.setType("Android");
                viewModel.setGankPage(1);
                loadGankData();
                break;
            case 2:
                viewModel.setType("all");
                viewModel.setGankPage(1);
                loadGankData();
                break;
            case 3:
                break;
            default:
                break;
        }
    }

    private void initViewModel(int position) {
        if (position == 0) {
            mAdapter = new CategoryArticleAdapter(this);
            MyDividerItemDecoration itemDecoration = new MyDividerItemDecoration(binding.recyclerView.getContext(), DividerItemDecoration.VERTICAL, false);
            itemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(binding.recyclerView.getContext(), R.drawable.shape_line)));
            binding.recyclerView.addItemDecoration(itemDecoration);
            binding.recyclerView.setAdapter(mAdapter);
        } else if (position == 1 || position == 2) {
            mAdapter = new GankAndroidSearchAdapter(this);
            MyDividerItemDecoration itemDecoration = new MyDividerItemDecoration(binding.recyclerView.getContext(), DividerItemDecoration.VERTICAL, false);
            itemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(binding.recyclerView.getContext(), R.drawable.shape_remove)));
            binding.recyclerView.addItemDecoration(itemDecoration);
            binding.recyclerView.setAdapter(mAdapter);
        }
        mAdapter.setOnLoadMoreListener(() -> {
            int position2 = binding.tlSearch.getSelectedTabPosition();
            if (position2 == 0) {
                int page = viewModel.getPage();
                viewModel.setPage(++page);
                loadWanData();
            } else if (position2 == 1 || position2 == 2) {
                int page = viewModel.getGankPage();
                viewModel.setGankPage(++page);
                loadGankData();
            }

        }, binding.recyclerView);
        if (!TextUtils.isEmpty(keyWord)) {
            load();
        }
    }

    private void initRefreshView() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setItemAnimator(null);
        int tabPosition = binding.tlSearch.getSelectedTabPosition();
        switch (tabPosition) {
            case 0:
                mAdapter = new CategoryArticleAdapter(this);
                break;
            case 1:
            case 2:
                mAdapter = new GankAndroidSearchAdapter(this);
                break;
            default:
                break;
        }
        mAdapter.bindToRecyclerView(binding.recyclerView, true);
    }

    private void loadWanData() {
        viewModel.searchWan(keyWord).observe(this, new Observer<HomeListBean>() {
            @Override
            public void onChanged(@Nullable HomeListBean homeListBean) {
                if (homeListBean != null
                        && homeListBean.getData() != null
                        && homeListBean.getData().getDatas() != null
                        && homeListBean.getData().getDatas().size() > 0) {
                    showHotTagView(false);
                    if (viewModel.getPage() == 0) {
                        mAdapter.getData().clear();
                        mAdapter.notifyDataSetChanged();
                    }
                    if (mAdapter instanceof CategoryArticleAdapter) {
                        mAdapter.addData(homeListBean.getData().getDatas());
                    }
                    mAdapter.loadMoreComplete();
                } else {
                    if (viewModel.getPage() != 0) {
                        mAdapter.loadMoreEnd();
                    } else {
                        mAdapter.getData().clear();
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    private void loadGankData() {
        viewModel.loadGankData(keyWord).observe(this, bean -> {
            if (bean != null && bean.getResults() != null && bean.getResults().size() > 0) {
                showHotTagView(false);
                if (viewModel.getGankPage() == 1) {
                    mAdapter.getData().clear();
                    mAdapter.notifyDataSetChanged();
                }
                if (mAdapter instanceof GankAndroidSearchAdapter) {
                    GankAndroidSearchAdapter adapter = (GankAndroidSearchAdapter) this.mAdapter;
                    adapter.addData(bean.getResults());
                    int position = binding.tlSearch.getSelectedTabPosition();
                    if (position == 2) {
                        adapter.setAllType(true);
                    } else {
                        adapter.setAllType(false);
                    }
                }
                mAdapter.loadMoreComplete();
            } else {
                if (viewModel.getGankPage() != 1) {
                    mAdapter.loadMoreEnd();
                } else {
                    mAdapter.getData().clear();
                    mAdapter.notifyDataSetChanged();
                    mAdapter.loadMoreComplete();
                }
            }
        });
    }

    private void showTagView(TagFlowLayout flowlayoutHot, final List<SearchTagBean.DataBean> beanList) {
        flowlayoutHot.removeAllViews();
        flowlayoutHot.setAdapter(new TagAdapter<SearchTagBean.DataBean>(beanList) {
            @Override
            public View getView(FlowLayout parent, int position, SearchTagBean.DataBean bean) {
                TextView textView = (TextView) View.inflate(parent.getContext(), R.layout.layout_navi_tag, null);
                textView.setText(Html.fromHtml(bean.getName()));
                return textView;
            }
        });
        flowlayoutHot.setOnTagClickListener(new TagFlowLayout.OnTagClickListener() {
            @Override
            public boolean onTagClick(View view, int position, FlowLayout parent) {
                viewModel.keyWord.set(beanList.get(position).getName());
                BaseTools.hideSoftKeyBoard(SearchActivity.this);
                return true;
            }
        });
    }

    private void showHotTagView(boolean isShow) {
        if (isShow) {
            binding.llSearchTag.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.llSearchTag.setVisibility(View.GONE);
        }
    }

    public static void start(Context mContext) {
        Intent intent = new Intent(mContext, SearchActivity.class);
        mContext.startActivity(intent);
    }
}
