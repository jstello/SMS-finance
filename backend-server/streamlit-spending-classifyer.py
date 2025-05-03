import streamlit as st
import pandas as pd
import numpy as np
import plotly.express as px 

st.set_page_config(page_title="Spending Classifier", page_icon=":money_with_wings:", layout="wide")

st.title("Spending Classifier")

@st.cache_data
def load_data():
    df = pd.read_csv("transactions_backup.csv")
    return df

@st.cache_data
def load_categories():
    df = pd.read_csv("categories_backup.csv")
    return df

df = load_data()

categories = load_categories()

category_map = categories.set_index("id")["name"].to_dict()

df["category"] = df["categoryId"].map(category_map)


# 

from st_aggrid import AgGrid

df['date'] = pd.to_datetime(df['date'])
df['year'] = df['date'].dt.year
df['month'] = df['date'].dt.month


# Filter by date range

selected_year = st.sidebar.selectbox("Year", df["year"].unique())

df = df[df["year"] == selected_year]

selected_month = st.sidebar.selectbox("Month", df["month"].unique())

df = df[df["month"] == selected_month]

# Amount range  

amount_range = st.sidebar.slider("Amount Range", min_value=df["amount"].min(), max_value=df["amount"].max(), value=(df["amount"].min(), df["amount"].max()))

# Filter by amount range
df = df[df["amount"].between(amount_range[0], amount_range[1])]

search_text = st.sidebar.text_input("Search Description", value="")

# Filter by search text
df = df[df["description"].str.contains(search_text, case=False)]

unique_providers = df["provider"].unique()

provider_filter = st.sidebar.multiselect("Provider", unique_providers)

if provider_filter: 
    # Filter by provider
    df = df[df["provider"].isin(provider_filter)]

unique_categories = df["category"].unique()

category_filter = st.sidebar.multiselect("Category", unique_categories)

if category_filter:
    # Filter by category
    df = df[df["categoryId"].isin(category_filter)]
    
category_values = df.groupby("category")["amount"].sum()

category_values = category_values.reset_index()

category_values.columns = ["category", "amount"]

fig_category = px.bar(category_values.sort_values('amount', ascending=False), x="category", y="amount", color="category", title=f"Category Totals for {selected_month}/{selected_year}")

selected_data = st.plotly_chart(fig_category, on_select="rerun")

selected_points = selected_data['selection']['points']

columns_to_show = ["date", "amount", "category", "provider", "income"]


if len(selected_points) > 0:
    selected_category = selected_points[0]['x']
    st.write(selected_category)

    df_filtered = df[df["category"] == selected_category]
else:
    df_filtered = df

    
st.write(df_filtered[columns_to_show])

# Provider Values

provider_values = df_filtered.groupby("provider")["amount"].sum()

provider_values = provider_values.reset_index()

provider_values.columns = ["provider", "amount"]

fig_provider = px.bar(provider_values.sort_values('amount', ascending=False), x="provider", y="amount", color="provider", title=f"Provider Totals for {selected_month}/{selected_year}")

selected_data = st.plotly_chart(fig_provider, on_select="rerun")

selected_points = selected_data['selection']['points']

if len(selected_points) > 0:
    selected_provider = selected_points[0]['x']
    st.write(selected_provider)

    df_filtered = df_filtered[df_filtered["provider"] == selected_provider]
    
total_expenses = df[df["income"] == False][df['category'] != "Investments"]["amount"].sum()

total_income = df[df["income"] == True]["amount"].sum()

col1, col2, col3, col4 = st.columns(4)

with col1:
    st.metric(label="Total expenses", value=f"{total_expenses/1e6:.2f} M COP")

with col2:
    st.metric(label="Total income", value=f"{total_income/1e6:.2f} M COP")
    
with col3:
    st.metric(label="Total savings", value=f"{total_income/1e6 - total_expenses/1e6:.2f} M COP", delta=f"{((total_income - total_expenses)/total_income*100):.2f}%")
with col4:
    total_investments = df[df['category'] == "Investments"]["amount"].sum()
    st.metric(label="Total investments", value=f"{total_investments/1e6:.2f} M COP", delta=f"{((total_investments)/total_income*100):.2f}%")









    
